package com.resky.yuaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.resky.yuaicodemother.ai.AiCodeGenAppNameService;
import com.resky.yuaicodemother.ai.AiCodeGenAppNameServiceFactory;
import com.resky.yuaicodemother.ai.AiCodeGenTypeRoutingService;
import com.resky.yuaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.resky.yuaicodemother.constant.AppConstant;
import com.resky.yuaicodemother.config.AiCostControlProperties;
import com.resky.yuaicodemother.core.AiCodeGeneratorFacade;
import com.resky.yuaicodemother.core.builder.VueProjectBuilder;
import com.resky.yuaicodemother.core.handler.StreamHandlerExecutor;
import com.resky.yuaicodemother.exception.BusinessException;
import com.resky.yuaicodemother.exception.ErrorCode;
import com.resky.yuaicodemother.exception.ThrowUtils;
import com.resky.yuaicodemother.model.dto.app.AppAddRequest;
import com.resky.yuaicodemother.model.dto.app.AppQueryRequest;
import com.resky.yuaicodemother.model.dto.aicost.AiCostReservation;
import com.resky.yuaicodemother.model.entity.App;
import com.resky.yuaicodemother.mapper.AppMapper;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.resky.yuaicodemother.model.enums.CodeGenTypeEnum;
import com.resky.yuaicodemother.model.enums.AppGenerationStatusEnum;
import com.resky.yuaicodemother.model.enums.AiUsageCallTypeEnum;
import com.resky.yuaicodemother.model.enums.AiUsageStatusEnum;
import com.resky.yuaicodemother.model.vo.AppVO;
import com.resky.yuaicodemother.model.vo.UserVO;
import com.resky.yuaicodemother.service.AppService;
import com.resky.yuaicodemother.service.AiCostControlService;
import com.resky.yuaicodemother.service.ChatHistoryService;
import com.resky.yuaicodemother.service.ScreenshotService;
import com.resky.yuaicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import dev.langchain4j.service.Result;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author resky
 */
@Slf4j
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {
    @Resource
    private UserService userService;

    @Resource
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotService screenshotService;

    @Resource
    private AiCodeGenAppNameService aiCodeGenAppNameService;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private AiCostControlService aiCostControlService;

    @Resource
    private AiCostControlProperties costControlProperties;

    /**
     * 生成代码（流式）
     *
     * @param appId       应用 ID
     * @param userMessage 用户提示词
     * @param loginUser   当前登录用户
     * @return 生成的代码
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String userMessage, User loginUser, String clientIp) {
        // 1.参数校验
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用 ID不能为空");
        ThrowUtils.throwIf(userMessage == null, ErrorCode.PARAMS_ERROR, "用户提示词不能为空");
        // 2.获取应用信息
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3.验证用户是否有权限访问该应用，仅本人可以生成代码
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无访问权限");
        // 4.获取应用的生成类型
        boolean initial = !AppGenerationStatusEnum.READY.name().equals(app.getGenerationStatus());
        int maxChars = initial ? costControlProperties.getInitialPromptMaxChars()
                : costControlProperties.getEditPromptMaxChars();
        ThrowUtils.throwIf(userMessage.length() > maxChars, ErrorCode.PARAMS_ERROR,
                "Prompt is too long (max " + maxChars + " characters)");
        // 必须先原子预占额度，再进行 AI 路由，避免额度不足时仍消耗路由 token
        AiCostReservation reservation = aiCostControlService.reserve(loginUser, appId, initial, clientIp);
        try {
            CodeGenTypeEnum codeGenTypeEnum = resolveCodeGenType(app, userMessage, loginUser);
            app.setGenerationStatus(AppGenerationStatusEnum.GENERATING.name());
            updateById(app);
            // 5. 通过校验后，添加用户消息到对话历史
            chatHistoryService.addChatMessage(appId, userMessage, ChatHistoryMessageTypeEnum.USER.getValue(), loginUser.getId());
            // 6. 调用 AI 生成代码（流式）
            Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, codeGenTypeEnum, appId, reservation);
            // 7.使用 AI 生成应用名称
            if ("新应用".equals(app.getAppName())) {
                Result<String> nameResult;
                try {
                    nameResult = aiCodeGenAppNameService.genAppName(userMessage);
                } catch (RuntimeException error) {
                    aiCostControlService.recordStandalone(loginUser, appId, AiUsageCallTypeEnum.APP_NAME,
                            "routing-model", null, AiUsageStatusEnum.FAILED, error);
                    throw error;
                }
                app.setAppName(nameResult.content());
                aiCostControlService.recordStandalone(loginUser, appId, AiUsageCallTypeEnum.APP_NAME,
                        "routing-model", nameResult.tokenUsage(), AiUsageStatusEnum.SUCCESS, null);
            }
            this.updateById(app);
            // 8. 收集 AI 响应内容并在完成后记录到对话历史
            return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, codeGenTypeEnum)
                    .doOnComplete(() -> updateGenerationStatus(appId, reservation.isBudgetExceeded()
                            ? AppGenerationStatusEnum.FAILED : AppGenerationStatusEnum.READY))
                    .doOnError(error -> updateGenerationStatus(appId, AppGenerationStatusEnum.FAILED));
        } catch (RuntimeException error) {
            aiCostControlService.fail(reservation, error, AiUsageStatusEnum.FAILED);
            updateGenerationStatus(appId, AppGenerationStatusEnum.FAILED);
            throw error;
        }

    }


    @Override
    public Long createApp(AppAddRequest appAddRequest, User loginUser) {
        // 参数校验
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(initPrompt != null && initPrompt.length() > costControlProperties.getInitialPromptMaxChars(),
                ErrorCode.PARAMS_ERROR,
                "Initial prompt max length is " + costControlProperties.getInitialPromptMaxChars() + " characters");
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化 prompt 不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtil.copyProperties(appAddRequest, app);
        app.setUserId(loginUser.getId());
        // 应用名称先使用占位，等代码生成结束后利用AI生成
        app.setAppName("新应用");
        // 路由延迟到首次生成且额度预占成功之后，创建应用本身不调用模型
        app.setCodeGenType(null);
        app.setGenerationStatus(AppGenerationStatusEnum.INIT.name());
        // 插入数据库
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        log.info("应用创建成功，ID: {}，等待首次生成时进行类型路由", app.getId());
        return app.getId();
    }

    private CodeGenTypeEnum resolveCodeGenType(App app, String userMessage, User loginUser) {
        CodeGenTypeEnum existingType = CodeGenTypeEnum.getEnumByValue(app.getCodeGenType());
        if (existingType != null) {
            return existingType;
        }
        if (StrUtil.isNotBlank(app.getCodeGenType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的生成类型");
        }
        String routingPrompt = StrUtil.isNotBlank(app.getInitPrompt()) ? app.getInitPrompt() : userMessage;
        AiCodeGenTypeRoutingService routingService = aiCodeGenTypeRoutingServiceFactory.createAiCodeGenTypeRoutingService();
        Result<CodeGenTypeEnum> routingResult;
        try {
            routingResult = routingService.routeCodeGenType(routingPrompt);
        } catch (RuntimeException error) {
            aiCostControlService.recordStandalone(loginUser, app.getId(), AiUsageCallTypeEnum.ROUTING,
                    "routing-model", null, AiUsageStatusEnum.FAILED, error);
            throw error;
        }
        CodeGenTypeEnum selectedType = routingResult.content();
        app.setCodeGenType(selectedType.getValue());
        updateById(app);
        aiCostControlService.recordStandalone(loginUser, app.getId(), AiUsageCallTypeEnum.ROUTING,
                "routing-model", routingResult.tokenUsage(), AiUsageStatusEnum.SUCCESS, null);
        return selectedType;
    }

    private void updateGenerationStatus(Long appId, AppGenerationStatusEnum status) {
        App update = new App();
        update.setId(appId);
        update.setGenerationStatus(status.name());
        updateById(update);
    }

    @Value("${code.deploy-host:http://localhost}")
    private String deployHost;

    /**
     * 部署应用
     *
     * @param appId     应用 ID
     * @param loginUser 当前登录用户
     * @return 部署的URL
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1.参数校验
        ThrowUtils.throwIf(appId == null, ErrorCode.PARAMS_ERROR, "应用 ID不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        // 2.查询应用信息
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3.验证用户是否有权限部署该应用，只有本人有权限部署
        ThrowUtils.throwIf(!app.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR, "无访问权限");
        // 4.检查是否已有deployKey
        String deployKey = app.getDeployKey();
        // 如果没有则生成 6 位 deployKey（大小写字母+数字）
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }
        // 5.获取代码生成类型，构建源项目路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 6.检查源目录是否存在
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "应用代码不存在，请先生成代码");
        }

        // 7. Vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // Vue 项目需要构建
            boolean buildSuccess = vueProjectBuilder.buildProject(sourceDirPath);
            ThrowUtils.throwIf(!buildSuccess, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDirPath, "dist");
            ThrowUtils.throwIf(!distDir.exists(), ErrorCode.SYSTEM_ERROR, "Vue 项目构建完成但未生成 dist 目录");
            // 将 dist 目录作为部署源
            sourceDir = distDir;
            log.info("Vue 项目构建成功，将部署 dist 目录: {}", distDir.getAbsolutePath());
        }
        // 8.复制文件到部署目录
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "部署失败：" + e.getMessage());
        }
        // 9.更新应用的 deployKey和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean updateResult = updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", deployHost, deployKey);
        // 11. 异步生成截图并更新应用封面
        generateAppScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;

    }

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    @Override
    public void generateAppScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.generateAndUploadScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }

    /**
     * 获取应用信息封装类
     *
     * @param app 应用实体类
     * @return 应用信息封装类
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 构造查询条件，根据传入的参数进行查询
     *
     * @param appQueryRequest 查询条件
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = appQueryRequest.getId();
        String appName = appQueryRequest.getAppName();
        String cover = appQueryRequest.getCover();
        String initPrompt = appQueryRequest.getInitPrompt();
        String codeGenType = appQueryRequest.getCodeGenType();
        String deployKey = appQueryRequest.getDeployKey();
        Integer priority = appQueryRequest.getPriority();
        Long userId = appQueryRequest.getUserId();
        String sortField = appQueryRequest.getSortField();
        String sortOrder = appQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .like("appName", appName)
                .like("cover", cover)
                .like("initPrompt", initPrompt)
                .eq("codeGenType", codeGenType)
                .eq("deployKey", deployKey)
                .eq("priority", priority)
                .eq("userId", userId)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 获取封装类列表
     *
     * @param appList 应用列表
     * @return 封装类列表
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 批量获取用户信息，避免 N+1 查询问题
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        Map<Long, UserVO> userVOMap = userService.listByIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, userService::getUserVO));
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 关联删除应用关联的对话数据
     *
     * @param id 应用 ID
     * @return 是否成功删除
     */
    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 转换为 long 类型
        long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }
        // 先删除关联的对话历史
        try {
            chatHistoryService.deleteByAppId(appId);
        } catch (Exception e) {
            // 记录日志但不阻止应用删除
            log.error("删除应用关联的对话历史失败：{}", e.getMessage());
        }
        // 删除应用
        return super.removeById(appId);
    }

}
