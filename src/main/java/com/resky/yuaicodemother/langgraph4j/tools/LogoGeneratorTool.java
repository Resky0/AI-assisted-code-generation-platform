package com.resky.yuaicodemother.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationOutput;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.resky.yuaicodemother.langgraph4j.model.ImageResource;
import com.resky.yuaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LogoGeneratorTool {

    @Value("${dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${dashscope.image-model:}")
    private String imageModel;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> logoList = new ArrayList<>();
        try {
            String logoPrompt = String.format("生成 Logo，Logo 中禁止包含任何文字！Logo 介绍：%s", description);
            MultiModalMessage userMessage = MultiModalMessage.builder()
                    .role(Role.USER.getValue())
                    .content(Arrays.asList(
                            Collections.singletonMap("text", logoPrompt)
                    ))
                    .build();
            MultiModalConversationParam param = MultiModalConversationParam.builder()
                    .apiKey(dashScopeApiKey)
                    .model(imageModel)
                    .messages(Collections.singletonList(userMessage))
                    .size("1024*1024")
                    .n(1)// 生成 1 张足够，因为 AI 不知道哪张最好
                    .promptExtend(true)
                    .watermark(false)
                    .negativePrompt("文字，字母，汉字，低分辨率，低画质，画面过饱和，蜡像感，过度光滑，画面具有AI感，构图混乱")
                    .build();
            MultiModalConversation conv = new MultiModalConversation();
            MultiModalConversationResult result = conv.call(param);
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null) {
                List<MultiModalConversationOutput.Choice> choices = result.getOutput().getChoices();
                for (MultiModalConversationOutput.Choice choice : choices) {
                    if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                        List<Map<String, Object>> contentList = choice.getMessage().getContent();
                        for (Map<String, Object> contentItem : contentList) {
                            Object imageObj = contentItem.get("image");
                            String imageUrl = imageObj != null ? imageObj.toString() : null;
                            if (StrUtil.isNotBlank(imageUrl)) {
                                logoList.add(ImageResource.builder()
                                        .category(ImageCategoryEnum.LOGO)
                                        .description(description)
                                        .url(imageUrl)
                                        .build());
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("生成 Logo 失败: {}", e.getMessage(), e);
        }
        return logoList;
    }
}
