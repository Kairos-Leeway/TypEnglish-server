package com.typenglish.service;

import com.typenglish.common.BusinessException;
import com.typenglish.entity.User;
import com.typenglish.mapper.UserMapper;
import com.typenglish.security.JwtInterceptor;
import org.springframework.stereotype.Service;

/**
 * 用户 XP/等级管理
 */
@Service
public class UserService {

    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public record XpInfo(int level, int xp, int totalXp, int gained) {}

    /** 获取经验，返回最新等级/经验 */
    public XpInfo gainXp(int amount) {
        Long uid = currentUserId();
        User u = userMapper.selectById(uid);
        if (u == null) throw new BusinessException("用户不存在");

        int level = u.getLevel() != null ? u.getLevel() : 1;
        int xp = u.getXp() != null ? u.getXp() : 0;
        int totalXp = u.getTotalXp() != null ? u.getTotalXp() : 0;
        int xpPerLevel = level * 100;

        xp += amount;
        totalXp += amount;
        while (xp >= xpPerLevel) {
            xp -= xpPerLevel;
            level++;
            xpPerLevel = level * 100;
        }

        u.setLevel(level);
        u.setXp(xp);
        u.setTotalXp(totalXp);
        userMapper.updateById(u);

        return new XpInfo(level, xp, totalXp, amount);
    }

    /** 获取当前等级/经验 */
    public XpInfo getXp() {
        Long uid = JwtInterceptor.CURRENT_USER.get();
        if (uid == null) return new XpInfo(1, 0, 0, 0);

        User u = userMapper.selectById(uid);
        if (u == null) return new XpInfo(1, 0, 0, 0);

        return new XpInfo(
                u.getLevel() != null ? u.getLevel() : 1,
                u.getXp() != null ? u.getXp() : 0,
                u.getTotalXp() != null ? u.getTotalXp() : 0,
                0
        );
    }

    private Long currentUserId() {
        Long uid = JwtInterceptor.CURRENT_USER.get();
        if (uid == null) throw new BusinessException(401, "未登录");
        return uid;
    }
}
