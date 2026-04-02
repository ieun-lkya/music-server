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

    //  🚀 核心战役：用 Java 组装楼中楼评论树！
    @GetMapping("/list")
    public Result<List<Comment>> list(@RequestParam Integer musicId) {
        // 1. 查出这首歌的所有评论
        List<Comment> allComments = commentMapper.selectByMusicId(musicId); // 请确保 Mapper 里有这个按 musicId 查询的方法
        
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
