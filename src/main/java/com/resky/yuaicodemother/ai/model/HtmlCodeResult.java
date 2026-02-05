package com.resky.yuaicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class HtmlCodeResult {
    @Description("生成的 HTML 代码")
    private String htmlCode;

    @Description("生成的 HTML 描述")
    private String description;
}
