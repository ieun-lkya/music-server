package com.music.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.util.Date;

@Data
public class Comment {
    private Integer id;
    private Integer musicId;
    private Integer userId;
    private String content;
    private Integer likes;
    
    // 极其贴心的时间格式化，传给前端直接是漂亮的字符串
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT+8")
    private Date createTime;
    
    // 联表查询时的冗余字段，方便前端直接展示头像和昵称
    private String username;
    private String avatar;
    
    // 树形结构需要的字段
    private Integer parentId; // 父评论 ID
    
    // 下面这两个字段数据库里没有，只用于后端组装楼中楼返回给前端
    private java.util.List<Comment> replies; // 子评论列表
    
    private String targetUsername; // 记录回复的是谁
}
