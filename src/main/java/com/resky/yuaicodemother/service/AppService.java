package com.resky.yuaicodemother.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.resky.yuaicodemother.model.dto.app.AppQueryRequest;
import com.resky.yuaicodemother.model.entity.App;
import com.resky.yuaicodemother.model.vo.AppVO;

import java.util.List;

/**
 * 应用 服务层。
 *
 * @author resky
 */
public interface AppService extends IService<App> {
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

    List<AppVO> getAppVOList(List<App> appList);
}
