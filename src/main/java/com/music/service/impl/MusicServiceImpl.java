package com.music.service.impl;

import com.music.entity.MusicInfo;
import com.music.mapper.MusicMapper;
import com.music.service.MusicService;
import com.music.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class MusicServiceImpl implements MusicService {

    @Autowired
    private MusicMapper musicMapper;

    @Autowired
    private OssService ossService;

    @Override
    public List<MusicInfo> getAllMusic() {
        return musicMapper.selectAll();
    }

    @Override
    public void addMusic(MusicInfo musicInfo) {
        musicMapper.insertMusic(musicInfo);
        log.info("====== [Service] 歌曲入库成功: {} ======", musicInfo.getTitle());
    }

    @Override
    public List<MusicInfo> recommendMusic(String sceneText) {
        String aiTag = mockAiExtractTag(sceneText);
        List<MusicInfo> matchedList = musicMapper.searchByTag(aiTag);
        if (matchedList == null || matchedList.isEmpty()) {
            return musicMapper.selectAll();
        }
        return matchedList;
    }

    private String mockAiExtractTag(String text) {
        if (text == null || text.trim().isEmpty()) { return "治愈"; }
        if (text.contains("雨") || text.contains("失恋")) return "伤感";
        if (text.contains("学习") || text.contains("书")) return "工作学习";
        if (text.contains("运动") || text.contains("跑")) return "运动";
        return "流行";
    }

    @Override
    public void deleteMusic(Long id) {
        MusicInfo oldMusic = musicMapper.getById(id);
        if (oldMusic != null) {
            if (oldMusic.getAudioUrl() != null) ossService.deleteFile(oldMusic.getAudioUrl());
            if (oldMusic.getCoverUrl() != null) ossService.deleteFile(oldMusic.getCoverUrl());
            if (oldMusic.getLyricUrl() != null) ossService.deleteFile(oldMusic.getLyricUrl());
        }
        musicMapper.deleteById(id);
    }

    // 🚀 核心修复：适配了新的 Mapper，消灭了红线报错！
    @Override
    public void updateCover(Long id, String newCoverUrl) {
        MusicInfo oldMusic = musicMapper.getById(id);
        if (oldMusic != null && oldMusic.getCoverUrl() != null) {
            ossService.deleteFile(oldMusic.getCoverUrl());
        }
        MusicInfo updateInfo = new MusicInfo();
        updateInfo.setId(id.intValue());
        updateInfo.setCoverUrl(newCoverUrl);
        musicMapper.updateMusic(updateInfo);
    }

    @Override
    public MusicInfo getById(Long id) {
        return musicMapper.getById(id);
    }

    @Override
    public void deleteById(Long id) {
        musicMapper.deleteById(id);
    }

    @Override
    public void updateMusic(MusicInfo music) {
        musicMapper.updateMusic(music);
    }
}