package com.resky.yuaicodemother.ai;


import com.resky.yuaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class AiCodeGenAppNameServiceFactoryTest {
    @Resource
    private AiCodeGenAppNameService aiCodeGenAppNameService;

    @Test
    public void testRouteCodeGenType() {
        String userPrompt = "做一个简单的个人介绍页面";
        String result = aiCodeGenAppNameService.genAppName(userPrompt).content();
        log.info("用户需求: {} -> {}",userPrompt,result);
        userPrompt ="做一个公司官网，需要首页、关于我们、联系我们三个页面";
        result =aiCodeGenAppNameService.genAppName(userPrompt).content();
        log.info("用户需求: {} -> {}",userPrompt,result);
        userPrompt ="做一个电商管理系统，包含用户管理、商品管理、订单管理，需要路由和状态管理";
        result =aiCodeGenAppNameService.genAppName(userPrompt).content();
        log.info("用户需求: {} -> {}",userPrompt,result);
    }
}
