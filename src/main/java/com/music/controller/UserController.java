package com.music.controller;

import com.music.common.Result;
import com.music.entity.MusicInfo;
import com.music.entity.User;
import com.music.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    // --- 【1. 用户登录/注册系统】 ---
    
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("账号密码不能为空");
        }
        User existUser = userMapper.findByUsername(user.getUsername());
        if (existUser != null) {
            return Result.error("账号已被注册，换一个吧");
        }
        // 给个默认头像
        user.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
        // 新增：给刚注册的用户发一个随机网名
        user.setNickname("音乐达人_" + (System.currentTimeMillis() % 10000)); 
        userMapper.insertUser(user);
        log.info("====== 新用户注册成功: {} ======", user.getUsername());
        return Result.success("注册成功，请登录！");
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user) {
        User dbUser = userMapper.findByUsername(user.getUsername());
        if (dbUser == null || !dbUser.getPassword().equals(user.getPassword())) {
            return Result.error("账号或密码错误");
        }
        dbUser.setPassword(null);
        // 核心：签发 JWT 令牌
        String token = com.music.utils.JwtUtils.generateToken(dbUser.getId(), dbUser.getUsername());
            
        Map<String, Object> res = new java.util.HashMap<>();
        res.put("user", dbUser);
        res.put("token", token);
            
        log.info("====== 用户登录成功并签发令牌：{} ======", dbUser.getUsername());
        return Result.success(res);
    }

    // --- 【2. 收藏业务系统】 ---

    @PostMapping("/like")
    public Result<String> likeMusic(@RequestParam("userId") Long userId, @RequestParam("musicId") Long musicId) {
        userMapper.likeMusic(userId, musicId);
        return Result.success("收藏成功！");
    }

    @PostMapping("/unlike")
    public Result<String> unlikeMusic(@RequestParam("userId") Long userId, @RequestParam("musicId") Long musicId) {
        userMapper.unlikeMusic(userId, musicId);
        return Result.success("已取消收藏");
    }

    @GetMapping("/likes")
    public Result<List<MusicInfo>> getMyLikes(@RequestParam("userId") Long userId) {
        List<MusicInfo> likedList = userMapper.getLikedMusicList(userId);
        return Result.success(likedList);
    }

    // --- 【3. 用户名片更新系统】 ---

    // 🚀 开放给 C 端的名片更新接口
    @PostMapping("/update")
    public Result<User> updateUserInfo(@RequestBody User user) {
        if (user.getId() == null) return Result.error("用户 ID 异常");
        
        userMapper.updateUser(user);
        // 更新完后，查出最新数据返回给前端刷新状态！
        User updatedUser = userMapper.selectById(user.getId());
        updatedUser.setPassword(null); // 极其严谨的数据安全，绝不把密码传回前端！
        return Result.success(updatedUser);
    }

    // --- 【4. 社交/搜索系统】 ---

    @GetMapping("/search")
    public Result<List<User>> searchUsers(@RequestParam("keyword") String keyword) {
        // 1. 极其严谨的判空拦截
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("搜索关键词不能为空");
        }
        
        // 2. 调用 Mapper 的模糊查询引擎
        List<User> users = userMapper.searchUsers(keyword.trim());
        
        // 3. 返回安全脱敏后的用户列表给前端
        return Result.success(users);
    }
}