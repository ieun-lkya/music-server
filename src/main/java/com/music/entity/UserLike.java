package com.music.entity;
import lombok.Data;
import java.util.Date;

@Data
public class UserLike {
    private Long id;
    private Long userId;
    private Long musicId;
    private Date createTime;
}