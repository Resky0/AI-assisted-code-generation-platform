package com.resky.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.resky.yuaicodemother.model.dto.app.AppAddRequest;
import com.resky.yuaicodemother.model.dto.app.AppQueryRequest;
import com.resky.yuaicodemother.model.entity.App;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author resky
 */
public interface AppService extends IService<App> {
    /**
     * 生成代码（流式
     *
     * @param appId       应用 ID
     * @param userMessage 用户提示词
     * @param loginUser   当前登录用户
     * @return 生成的代码
     */
    Flux<String> chatToGenCode(Long appId, String userMessage, User loginUser, String clientIp);

    Long createApp(AppAddRequest appAddRequest, User loginUser);

    /**
     * 部署应用
     *
     * @param appId     应用 ID
     * @param loginUser 当前登录用户
     * @return 部署的URL
     */
    String deployApp(Long appId, User loginUser);

    void generateAppScreenshotAsync(Long appId, String appUrl);

    /**
     * 获取应用信息封装类
     *
     * @param app 应用实体类
     * @return 应用信息封装类
     */
    AppVO getAppVO(App app);

    /**
     * 构造查询条件，根据传入的参数进行查询
     *
     * @param appQueryRequest 查询条件
     * @return
     */
    QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest);

    /**
     * 获取封装类列表
     *
     * @param appList 应用列表
     * @return 封装类列表
     */
    List<AppVO> getAppVOList(List<App> appList);
}
