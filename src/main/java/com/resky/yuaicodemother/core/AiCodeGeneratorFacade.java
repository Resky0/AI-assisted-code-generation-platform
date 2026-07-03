package com.resky.yuaicodemother.core;

import cn.hutool.json.JSONUtil;
import com.resky.yuaicodemother.ai.AiCodeGeneratorService;
import com.resky.yuaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.resky.yuaicodemother.ai.model.HtmlCodeResult;
import com.resky.yuaicodemother.ai.model.MultiFileCodeResult;
import com.resky.yuaicodemother.ai.model.message.AiResponseMessage;
import com.resky.yuaicodemother.ai.model.message.ToolExecutedMessage;
import com.resky.yuaicodemother.ai.model.message.ToolRequestMessage;
import com.resky.yuaicodemother.constant.AppConstant;
import com.resky.yuaicodemother.core.builder.VueProjectBuilder;
import com.resky.yuaicodemother.core.parser.CodeParserExecutor;
import com.resky.yuaicodemother.core.saver.CodeFileSaverExecutor;
import com.resky.yuaicodemother.exception.BusinessException;
import com.resky.yuaicodemother.exception.ErrorCode;
import com.resky.yuaicodemother.exception.AiBudgetExceededException;
import com.resky.yuaicodemother.model.dto.aicost.AiCostReservation;
import com.resky.yuaicodemother.model.enums.AiUsageStatusEnum;
import com.resky.yuaicodemother.service.AiCostControlService;
import com.resky.yuaicodemother.config.AiCostControlProperties;
import dev.langchain4j.service.TokenStreamLimitException;
import com.resky.yuaicodemother.model.enums.CodeGenTypeEnum;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI代码生成门面类，组合生成和保存功能
 */
@Slf4j
@Service
public class AiCodeGeneratorFacade {
    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private AiCostControlService aiCostControlService;

    @Resource
    private AiCostControlProperties costControlProperties;

    /**
     * 统一入口，根据类型生成并保存代码
     *
     * @param userMessage     用户提示词
     * @param codeGenTypeEnum 生成类型
     * @return 保存的目录
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenTypeEnum, Long appId) {
        if (codeGenTypeEnum == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的AI服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId);
        return switch (codeGenTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaver.saveHtmlCodeResult(htmlCodeResult, appId);

            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaver.saveMultiFileCodeResult(multiFileCodeResult, appId);
            }

            default -> {
                String errorMessage = "不支持的生成类型" + codeGenTypeEnum.getValue();
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, errorMessage);
            }
        };
    }


    /**
     * 统一入口，根据类型生成并保存代码（流式）
     *
     * @param userMessage 用户提示词
     * @param codeGenType 生成类型
     * @return 保存的目录
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId,
                                                   AiCostReservation reservation) {
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成类型为空");
        }
        // 根据 appId 获取对应的AI服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, codeGenType);
        return switch (codeGenType) {
            case HTML -> {
                TokenStream codeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                Flux<String> textStream = processTextTokenStream(codeStream, reservation);
                yield processCodeStream(textStream, CodeGenTypeEnum.HTML, appId, reservation);
            }
            case MULTI_FILE -> {
                TokenStream codeStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                Flux<String> textStream = processTextTokenStream(codeStream, reservation);
                yield processCodeStream(textStream, CodeGenTypeEnum.MULTI_FILE, appId, reservation);
            }
            case VUE_PROJECT -> {
                TokenStream codeStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(codeStream, appId, reservation);
            }
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型");
        };
    }

    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, Long appId) {
        return generateAndSaveCodeStream(userMessage, codeGenType, appId, null);
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId, AiCostReservation reservation) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, toolExecutionRequest) -> {
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(toolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })
                    .onRoundUsage((usage, toolRounds) -> {
                        if (reservation != null) aiCostControlService.reportUsage(reservation, usage, toolRounds);
                    })
                    .maxTotalTokens(reservation == null ? Long.MAX_VALUE : reservation.getMaxTokens())
                    .maxToolRounds(reservation == null ? Integer.MAX_VALUE : reservation.getMaxToolRounds())
                    .maxDuration(costControlProperties.getMaxExecutionTime())
                    .onCompleteResponse((ChatResponse response) -> {
                        // 异步构造 Vue 项目
                        String projectPath = AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId;
                        vueProjectBuilder.buildProjectAsync(projectPath);
                        if (reservation != null) aiCostControlService.complete(reservation);
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        if (reservation != null && (error instanceof AiBudgetExceededException || error instanceof TokenStreamLimitException)) {
                            reservation.setBudgetExceeded(true);
                            aiCostControlService.fail(reservation, error, AiUsageStatusEnum.BUDGET_EXCEEDED);
                            sink.next(JSONUtil.toJsonStr(new AiResponseMessage("\n\n任务已达到本次额度上限，已保留当前生成结果。")));
                            sink.complete();
                        } else {
                            if (reservation != null) aiCostControlService.fail(reservation, error, AiUsageStatusEnum.FAILED);
                            sink.error(error);
                        }
                    })
                    .start();
        });
    }

    private Flux<String> processTextTokenStream(TokenStream tokenStream, AiCostReservation reservation) {
        return Flux.create(sink -> tokenStream
                .onPartialResponse(sink::next)
                .onRoundUsage((usage, toolRounds) -> {
                    if (reservation != null) aiCostControlService.reportUsage(reservation, usage, toolRounds);
                })
                .maxTotalTokens(reservation == null ? Long.MAX_VALUE : reservation.getMaxTokens())
                .maxToolRounds(reservation == null ? Integer.MAX_VALUE : reservation.getMaxToolRounds())
                .maxDuration(costControlProperties.getMaxExecutionTime())
                .onCompleteResponse(response -> {
                    if (reservation != null) aiCostControlService.complete(reservation);
                    sink.complete();
                })
                .onError(error -> {
                    if (reservation != null && (error instanceof AiBudgetExceededException
                            || error instanceof TokenStreamLimitException)) {
                        reservation.setBudgetExceeded(true);
                        aiCostControlService.fail(reservation, error, AiUsageStatusEnum.BUDGET_EXCEEDED);
                        sink.next("\n\n任务已达到本次额度上限，已停止继续生成。");
                        sink.complete();
                    } else {
                        if (reservation != null) {
                            aiCostControlService.fail(reservation, error, AiUsageStatusEnum.FAILED);
                        }
                        sink.error(error);
                    }
                })
                .start());
    }


    /**
     * 通用流式代码处理方法
     *
     * @param codeStream  代码流
     * @param codeGenType 代码生成类型
     * @return 流式响应
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, Long appId,
                                           AiCostReservation reservation) {
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
                    // 实时收集代码片段
                    codeBuilder.append(chunk);
                })
                .doOnComplete(() -> {
                    if (reservation != null && reservation.isBudgetExceeded()) {
                        log.info("任务达到预算上限，跳过保存不完整的 {} 代码，appId={}", codeGenType, appId);
                        return;
                    }
                    // 流式返回完成后保存代码
                    try {
                        String completeCode = codeBuilder.toString();
                        Object parsedResult = CodeParserExecutor.executeParser(completeCode, codeGenType);
                        // 保存代码到文件
                        File savedDir = CodeFileSaverExecutor.executeSaver(parsedResult, codeGenType, appId);
                        log.info("保存成功，路径为：" + savedDir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("保存失败：{}", e.getMessage());
                    }
                });
    }
}
