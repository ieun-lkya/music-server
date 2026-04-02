package com.music.controller;

import com.music.common.Result;
import com.music.entity.Comment;
import com.music.mapper.CommentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentMapper commentMapper;

    //  核心战役：用 Java 组装楼中楼评论树！
    //  修复白屏：配置双路由，同时兼容 /comment/list/12 和 /comment/list?musicId=12 两种请求！
    @GetMapping({"/list", "/list/{musicId}"})
    public Result<List<Comment>> list(
            @PathVariable(required = false) Integer musicId,
            @RequestParam(value = "musicId", required = false) Integer queryMusicId) {

        // ️ 智能参数捕获：不管是前端哪种风格发来的 ID，统统完美接住！
        Integer finalMusicId = musicId != null ? musicId : queryMusicId;

        // 1. 查出这首歌的所有评论
        List<Comment> allComments = commentMapper.selectByMusicId(finalMusicId); 
        
        List<Comment> rootComments = new java.util.ArrayList<>();
        java.util.Map<Integer, Comment> map = new java.util.HashMap<>();
        
        // 2. 初始化子评论列表并放入 Hash 字典，时间复杂度 O(N)
        for (Comment c : allComments) {
            c.setReplies(new java.util.ArrayList<>());
            map.put(c.getId(), c);
        }
        
        // 3. 极其优雅的树形重组
        for (Comment c : allComments) {
            if (c.getParentId() == null || c.getParentId() == 0) {
                rootComments.add(c); // 这是根评论
            } else {
                Comment parent = map.get(c.getParentId());
                if (parent != null) {
                    c.setTargetUsername(parent.getUsername()); // 记录是在回复谁
                    parent.getReplies().add(c); // 塞进父评论的肚子里
                }
            }
        }
        return Result.success(rootComments);
    }

    @PostMapping("/add")
    public Result<?> add(@RequestBody Comment comment) {
        comment.setCreateTime(new java.util.Date()); // 如果数据库没有自动生成时间的话
        commentMapper.insert(comment); // 请确保你的 insert SQL 已经加上了 parent_id 字段！
        return Result.success(null);
    }

    //  接收前端点赞冲击波
    @PostMapping("/like/{id}")
    public Result<?> likeComment(@PathVariable Integer id) {
        commentMapper.incrementLikes(id);
        return Result.success(null);
    }
}
