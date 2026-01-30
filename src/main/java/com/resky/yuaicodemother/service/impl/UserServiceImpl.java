package com.resky.yuaicodemother.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.resky.yuaicodemother.exception.BusinessException;
import com.resky.yuaicodemother.exception.ErrorCode;
import com.resky.yuaicodemother.exception.ThrowUtils;
import com.resky.yuaicodemother.model.dto.user.UserQueryRequest;
import com.resky.yuaicodemother.model.entity.User;
import com.resky.yuaicodemother.mapper.UserMapper;
import com.resky.yuaicodemother.model.enums.UserRoleEnum;
import com.resky.yuaicodemother.model.vo.LoginUserVO;
import com.resky.yuaicodemother.model.vo.UserVO;
import com.resky.yuaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;

import static com.resky.yuaicodemother.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author resky
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    /**
     * 用户注册
     *
     * @param userAccount   账号
     * @param userPassword  密码
     * @param checkPassword 确认密码
     * @return 用户ID
     */
    @Override
    public long register(String userAccount, String userPassword, String checkPassword) {
        // 校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword, checkPassword), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码长度过短");
        ThrowUtils.throwIf(!userPassword.equals(checkPassword), ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        // 验证账户不能重复
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq(User::getUserAccount, userAccount);
        long count = this.mapper.selectCountByQuery(wrapper);
        ThrowUtils.throwIf(count > 0, ErrorCode.OPERATION_ERROR, "账号已存在");
        // 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 插入数据
        User user = User.builder()
                .userAccount(userAccount)
                .userPassword(encryptPassword)
                .userName("无名")
                .userRole(UserRoleEnum.USER.getValue())
                .build();
        boolean saveResult = this.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR, "注册失败");
        return user.getId();

    }

    /**
     * 用户登录
     *
     * @param userAccount  账号
     * @param userPassword 密码
     * @return 用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 校验参数
        ThrowUtils.throwIf(StrUtil.hasBlank(userAccount, userPassword), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(userAccount.length() < 4, ErrorCode.PARAMS_ERROR, "账号长度过短");
        ThrowUtils.throwIf(userPassword.length() < 8, ErrorCode.PARAMS_ERROR, "密码长度过短");
        // 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户
        QueryWrapper wrapper = new QueryWrapper();
        wrapper.eq(User::getUserAccount, userAccount)
                .eq(User::getUserPassword, encryptPassword);
        User user = this.mapper.selectOneByQuery(wrapper);
        ThrowUtils.throwIf(user == null, ErrorCode.OPERATION_ERROR, "用户不存在或密码错误");
        // 记录用户信息
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 用户信息脱敏
        return getLoginUserVO(user);
    }

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return 脱敏的已登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return 登录用户信息
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已经登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        // 从数据库查询
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        ThrowUtils.throwIf(currentUser == null, ErrorCode.SYSTEM_ERROR, "用户不存在");
        return currentUser;
    }

    /**
     * 获取脱敏后的用户信息
     *
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        ThrowUtils.throwIf(user == null, ErrorCode.PARAMS_ERROR, "用户不存在");
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).toList();

    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 判断当前用户是否登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    /**
     * 封装用户查询对象
     * @param userQueryRequest  查询请求
     * @return
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }


    /**
     * 加密密码
     *
     * @param userPassword 原密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "resky";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
}
