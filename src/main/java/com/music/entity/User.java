package com.music.entity;
import lombok.Data;
import java.util.Date;


@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String avatar;
    private Date createTime;
}