package com.music.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor  // 一键生成无参构造 (MyBatis/JSON 序列化必备)
@AllArgsConstructor // 一键生成全参构造
@Builder            // 开启 Builder 模式，以后 new 对象超爽
public class MusicInfo {
    private Integer id;
    private String title;
    private String artist;
    private String coverUrl;
    private String audioUrl;
    private String tags;
    private LocalDateTime createTime;
    private String lyricUrl;
}