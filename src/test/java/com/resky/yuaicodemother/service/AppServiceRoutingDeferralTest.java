package com.resky.yuaicodemother.service;

import com.resky.yuaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.resky.yuaicodemother.config.AiCostControlProperties;
import com.resky.yuaicodemother.exception.BusinessException;
import com.resky.yuaicodemother.exception.ErrorCode;
import com.resky.yuaicodemother.model.dto.app.AppAddRequest;
import com.resky.yuaicodemother.model.entity.App;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.enums.AppGenerationStatusEnum;
import com.resky.yuaicodemother.service.impl.AppServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppServiceRoutingDeferralTest {
    @Test
    void createAppDoesNotCallRoutingModel() {
        AppServiceImpl service = spy(new AppServiceImpl());
        AiCodeGenTypeRoutingServiceFactory routingFactory = mock(AiCodeGenTypeRoutingServiceFactory.class);
        ReflectionTestUtils.setField(service, "costControlProperties", new AiCostControlProperties());
        ReflectionTestUtils.setField(service, "aiCodeGenTypeRoutingServiceFactory", routingFactory);
        AtomicReference<App> savedApp = new AtomicReference<>();
        doAnswer(invocation -> {
            App app = invocation.getArgument(0);
            app.setId(123L);
            savedApp.set(app);
            return true;
        }).when(service).save(any(App.class));
        AppAddRequest request = new AppAddRequest();
        request.setInitPrompt("生成个人介绍网站");
        User user = user(1L);

        assertEquals(123L, service.createApp(request, user));
        verifyNoInteractions(routingFactory);
        assertNull(savedApp.get().getCodeGenType());
        assertEquals(AppGenerationStatusEnum.INIT.name(), savedApp.get().getGenerationStatus());
    }

    @Test
    void exhaustedQuotaStopsBeforeRouting() {
        AppServiceImpl service = spy(new AppServiceImpl());
        AiCostControlService costService = mock(AiCostControlService.class);
        AiCodeGenTypeRoutingServiceFactory routingFactory = mock(AiCodeGenTypeRoutingServiceFactory.class);
        ReflectionTestUtils.setField(service, "costControlProperties", new AiCostControlProperties());
        ReflectionTestUtils.setField(service, "aiCostControlService", costService);
        ReflectionTestUtils.setField(service, "aiCodeGenTypeRoutingServiceFactory", routingFactory);
        App app = new App();
        app.setId(10L);
        app.setUserId(1L);
        app.setInitPrompt("生成个人介绍网站");
        app.setGenerationStatus(AppGenerationStatusEnum.INIT.name());
        doReturn(app).when(service).getById(10L);
        when(costService.reserve(any(), eq(10L), eq(true), anyString()))
                .thenThrow(new BusinessException(ErrorCode.AI_QUOTA_EXCEEDED));

        assertThrows(BusinessException.class,
                () -> service.chatToGenCode(10L, "生成个人介绍网站", user(1L), "127.0.0.1"));
        verifyNoInteractions(routingFactory);
    }

    private static User user(long id) {
        User user = new User();
        user.setId(id);
        user.setUserRole("user");
        return user;
    }
}
