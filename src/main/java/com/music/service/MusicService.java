// src/main/java/com/music/service/MusicService.java
package com.music.service;

import com.music.entity.MusicInfo;
import java.util.List;

/**
 * 音乐业务逻辑接口
 */
public interface MusicService {

    /**
     * 获取全量音乐列表
     */
    List<MusicInfo> getAllMusic();

    void addMusic(MusicInfo musicInfo);
    /**
     * 根据场景文本推荐音乐
     */
    List<MusicInfo> recommendMusic(String sceneText);
    void deleteMusic(Long id);
    void updateCover(Long id, String coverUrl);
    MusicInfo getById(Long id);
    void deleteById(Long id);
    void updateMusic(MusicInfo music);
}