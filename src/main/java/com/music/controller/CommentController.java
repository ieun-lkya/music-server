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

    //  发布评论接口
    @PostMapping("/add")
    public Result<?> addComment(@RequestBody Comment comment) {
        if (comment.getMusicId() == null || comment.getUserId() == null || comment.getContent() == null) {
            return Result.error("评论参数不完整！");
        }
        commentMapper.insert(comment);
        return Result.success();
    }

    //  获取某首歌的热评列表接口
    @GetMapping("/list")
    public Result<List<Comment>> getList(@RequestParam Integer musicId) {
        List<Comment> list = commentMapper.selectByMusicId(musicId);
        return Result.success(list);
    }
}
