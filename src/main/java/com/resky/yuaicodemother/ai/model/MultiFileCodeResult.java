package com.resky.yuaicodemother.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

@Data
public class MultiFileCodeResult {
    @Description("HTML 代码")
    private String htmlCode;
    @Description("CSS 代码")
    private String cssCode;
    @Description("JS 代码")
    private String jsCode;
    @Description("生成的项目描述")
    private String description;
}
