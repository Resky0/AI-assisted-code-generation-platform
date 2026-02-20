package com.resky.yuaicodemother.service;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.resky.yuaicodemother.model.dto.chatHistory.ChatHistoryQueryRequest;
import com.resky.yuaicodemother.model.entity.ChatHistory;
import com.resky.yuaicodemother.model.entity.User;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;

import java.time.LocalDateTime;

/**
 * 对话历史 服务层。
 *
 * @author resky
 */
public interface ChatHistoryService extends IService<ChatHistory> {
    /**
     * 加载聊天记录到内存中
     *
     * @param appId      应用 ID
     * @param chatMemory 对话记忆对象
     * @param maxCount   最大加载条数
     * @return 加载的聊天记录数量
     */
    int loadChatHistoryToMemory(Long appId, MessageWindowChatMemory chatMemory, int maxCount);

    /**
     * 添加聊天消息到历史记录中。
     *
     * @param appId       应用ID，不能为空
     * @param message     消息内容，不能为空
     * @param messageType 消息类型，不能为空
     * @param userId      用户ID，不能为空
     * @return 是否成功保存聊天记录
     */
    boolean addChatMessage(Long appId, String message, String messageType, Long userId);

    /**
     * 游标分页查询AI 对话记录
     *
     * @param appId          应用 ID
     * @param pageSize       页面大小
     * @param lastCreateTime 上次创建时间
     * @param loginUser      登录用户
     * @return 聊天记录分页数据
     */
    Page<ChatHistory> listAppChatHistoryByPage(Long appId, int pageSize,
                                               LocalDateTime lastCreateTime,
                                               User loginUser);

    /**
     * 获取查询包装类
     *
     * @param chatHistoryQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(ChatHistoryQueryRequest chatHistoryQueryRequest);

    /**
     * 根据应用ID删除记录。
     *
     * @param appId 应用ID，必须大于0，否则抛出参数错误异常
     * @return 删除成功返回true，否则返回false
     */
    boolean deleteByAppId(Long appId);
}
