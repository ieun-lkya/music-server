package com.music.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.music.common.Result;
import com.music.entity.MusicInfo;
import com.music.service.MusicService;
import com.music.service.OssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/admin")
public class AdminController {

    private static final String AI_API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    @Autowired
    private OssService ossService;

    @Autowired
    private MusicService musicService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/dashboard/data")
    public Result<Map<String, Object>> getDashboardData() {
        Map<String, Object> data = new HashMap<>();
        try {
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_info", Integer.class);
            Integer musicCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM music_info", Integer.class);
            Integer likeCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_likes", Integer.class);

            data.put("userCount", userCount != null ? userCount : 0);
            data.put("musicCount", musicCount != null ? musicCount : 0);
            data.put("likeCount", likeCount != null ? likeCount : 0);

            String topSql = "SELECT m.title AS title, COUNT(l.music_id) AS likes " +
                    "FROM music_info m " +
                    "LEFT JOIN user_likes l ON m.id = l.music_id " +
                    "GROUP BY m.id, m.title " +
                    "ORDER BY likes DESC, m.id DESC LIMIT 5";
            List<Map<String, Object>> topSongs = jdbcTemplate.queryForList(topSql);
            data.put("topSongs", topSongs);

            List<String> tagsList = jdbcTemplate.queryForList("SELECT tags FROM music_info WHERE tags IS NOT NULL AND tags != ''", String.class);
            Map<String, Integer> tagMap = new HashMap<>();
            for (String tags : tagsList) {
                for (String tag : tags.split("[,，]")) {
                    String t = tag.trim();
                    if (!t.isEmpty()) tagMap.put(t, tagMap.getOrDefault(t, 0) + 1);
                }
            }
            List<Map<String, Object>> tagDistribution = new ArrayList<>();
            tagMap.forEach((k, v) -> {
                Map<String, Object> item = new HashMap<>();
                item.put("name", k);
                item.put("value", v);
                tagDistribution.add(item);
            });
            data.put("tagDistribution", tagDistribution);

            return Result.success(data);

        } catch (Exception e) {
            log.error("大屏数据查询异常", e);
            return Result.error("大屏数据获取失败，请查看后端控制台报错");
        }
    }

    @PostMapping("/parse")
    public Result<Map<String, String>> parseOnly(@RequestParam("file") MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".mp3")) {
            return Result.error("非 MP3 格式，请手动填写档案");
        }

        Map<String, String> musicInfoMap = new HashMap<>();
        File tempFile = null;
        try {
            tempFile = File.createTempFile("temp_music_", ".mp3");
            try (java.io.InputStream is = file.getInputStream()) {
                java.nio.file.Files.copy(is, tempFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            Mp3File mp3file = new Mp3File(tempFile.getAbsolutePath());

            // 🚀 核心修复 1：强行剥离 ID3 标签自带的幽灵空格，防止后续 URL 污染！
            String title = file.getOriginalFilename().replace(".mp3", "").trim();
            String artist = "未知歌手";
            String coverBase64 = "";

            if (mp3file.hasId3v2Tag()) {
                ID3v2 id3v2Tag = mp3file.getId3v2Tag();
                if (id3v2Tag.getTitle() != null && !id3v2Tag.getTitle().trim().isEmpty()) title = id3v2Tag.getTitle().trim();
                if (id3v2Tag.getArtist() != null && !id3v2Tag.getArtist().trim().isEmpty()) artist = id3v2Tag.getArtist().trim();
                byte[] albumImageData = id3v2Tag.getAlbumImage();
                if (albumImageData != null) {
                    String mimeType = id3v2Tag.getAlbumImageMimeType();
                    if (mimeType == null || mimeType.isEmpty()) mimeType = "image/jpeg";
                    coverBase64 = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(albumImageData);
                }
            }
            musicInfoMap.put("title", title);
            musicInfoMap.put("artist", artist);
            musicInfoMap.put("coverBase64", coverBase64);
            return Result.success(musicInfoMap);
        } catch (Exception e) {
            return Result.error("文件解析失败");
        } finally {
            if (tempFile != null && tempFile.exists()) tempFile.delete();
        }
    }

    @GetMapping("/suggestTags")
    public Result<String> suggestTags(@RequestParam String title, @RequestParam(required = false, defaultValue = "未知") String artist) {
        String API_KEY = System.getenv("DASHSCOPE_API_KEY");
        if (API_KEY == null || API_KEY.isEmpty()) {
            return Result.error("AI 引擎未授权，请配置 DASHSCOPE_API_KEY");
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(API_KEY);

            String validTagsStr = "流行,摇滚,民谣,电子,纯音乐,伤感,治愈,励志,轻松,助眠,运动,驾车,雨天,深夜,咖啡馆,工作学习,华语,欧美,日韩,怀旧,爱情,浪漫,节奏控,古风,经典";
            java.util.List<String> VALID_TAGS = java.util.Arrays.asList(validTagsStr.split(","));

            String prompt = String.format(
                    "你是一个极其专业的音乐打标机器。请根据歌曲名称《%s》和演唱者【%s】，挑选出所有符合该歌曲意境的贴切标签（数量不限，宁缺毋滥）。\n" +
                            "【强制候选词库】：%s\n" +
                            "【输出格式】：只输出标签，用英文逗号分隔。绝对禁止输出其他解释说明！",
                    title, artist, validTagsStr
            );

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", "qwen-plus");
            Map<String, String> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", prompt);
            requestBodyMap.put("messages", new Object[]{message});

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBodyMap, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(AI_API_URL, entity, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            String aiResponse = root.path("choices").get(0).path("message").path("content").asText().trim();

            String[] splitTags = aiResponse.split("[,，]");
            java.util.List<String> finalTags = new java.util.ArrayList<>();
            for (String tag : splitTags) {
                tag = tag.trim();
                if (VALID_TAGS.contains(tag)) finalTags.add(tag);
            }
            if (finalTags.isEmpty()) finalTags.add("流行");
            return Result.success(String.join(",", finalTags));
        } catch (Exception e) {
            return Result.error("AI 打标失败，请手动填写标签");
        }
    }

    @PostMapping("/publish")
    public Result<String> publishMusic(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "coverFile", required = false) MultipartFile coverFile,
            @RequestParam(value = "lyricFile", required = false) MultipartFile lyricFile,
            @RequestParam("title") String title,
            @RequestParam("artist") String artist,
            @RequestParam("tags") String tags) {
    
        try {
            String checkSql = "SELECT COUNT(*) FROM music_info WHERE title = ? AND artist = ?";
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, title, artist);
            if (count != null && count > 0) {
                return Result.error("拒绝上传：该歌手的《" + title + "》已存在曲库中！");
            }
    
            //  终极修复：把歌手名加回来！同时用正则清洗掉空格，用 - 连接！
            String cleanTitle = title.replaceAll("\\s+", ""); 
            String cleanArtist = artist.replaceAll("\\s+", "");
            // 现在的命名格式：黄昏晓 - 王心凌_a1b2c3
            String safeBaseName = cleanTitle + "-" + cleanArtist + "_" + UUID.randomUUID().toString().substring(0, 6);
    
            String audioExt = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
            String audioUrl = ossService.uploadFileWithCustomName(file, "songs/", safeBaseName + audioExt);
    
            String coverUrl = "";
            if (coverFile != null && !coverFile.isEmpty()) {
                String coverExt = coverFile.getOriginalFilename().substring(coverFile.getOriginalFilename().lastIndexOf("."));
                coverUrl = ossService.uploadFileWithCustomName(coverFile, "covers/", safeBaseName + coverExt);
            }
    
            String lyricUrl = "";
            if (lyricFile != null && !lyricFile.isEmpty()) {
                lyricUrl = ossService.uploadFileWithCustomName(lyricFile, "lyrics/", safeBaseName + ".lrc");
            }
    
            MusicInfo musicInfo = new MusicInfo();
            musicInfo.setTitle(title.trim());
            musicInfo.setArtist(artist.trim());
            musicInfo.setTags(tags);
            musicInfo.setAudioUrl(audioUrl);
            musicInfo.setCoverUrl(coverUrl);
            musicInfo.setLyricUrl(lyricUrl);
    
            musicService.addMusic(musicInfo);
            return Result.success("发布成功");
        } catch (Exception e) {
            log.error("发布失败", e);
            return Result.error("发布失败");
        }
    }

    @DeleteMapping("/delete/{id}")
    public Result<String> deleteMusic(@PathVariable Long id) {
        MusicInfo oldMusic = musicService.getById(id);
        if (oldMusic == null) return Result.error("歌曲不存在");

        musicService.deleteById(id);

        if (oldMusic.getCoverUrl() != null && !oldMusic.getCoverUrl().isEmpty()) ossService.deleteFile(oldMusic.getCoverUrl());
        if (oldMusic.getAudioUrl() != null && !oldMusic.getAudioUrl().isEmpty()) ossService.deleteFile(oldMusic.getAudioUrl());
        if (oldMusic.getLyricUrl() != null && !oldMusic.getLyricUrl().isEmpty()) ossService.deleteFile(oldMusic.getLyricUrl());

        return Result.success("源文件已彻底删除！");
    }

    @PostMapping("/update")
    public Result<String> updateMusic(
            MusicInfo form,
            @RequestParam(value = "newCover", required = false) MultipartFile newCover,
            @RequestParam(value = "newAudio", required = false) MultipartFile newAudio,
            @RequestParam(value = "newLyric", required = false) MultipartFile newLyric) {

        MusicInfo oldMusic = musicService.getById(form.getId().longValue());
        if (oldMusic == null) return Result.error("歌曲不存在");

        String title = form.getTitle() != null ? form.getTitle().trim() : oldMusic.getTitle().trim();
        //  终极修复：把误删的获取歌手名逻辑加回来！
        String artist = form.getArtist() != null ? form.getArtist().trim() : oldMusic.getArtist().trim();
        
        //  终极修复：更新时同样使用 [歌名 - 歌手_随机码] 的完美格式！
        String cleanTitle = title.replaceAll("\\s+", ""); 
        String cleanArtist = artist.replaceAll("\\s+", "");
        String safeBaseName = cleanTitle + "-" + cleanArtist + "_" + UUID.randomUUID().toString().substring(0, 6);

        if (newCover != null && !newCover.isEmpty()) {
            String ext = newCover.getOriginalFilename().substring(newCover.getOriginalFilename().lastIndexOf("."));
            form.setCoverUrl(ossService.uploadFileWithCustomName(newCover, "covers/", safeBaseName + ext));
            if (oldMusic.getCoverUrl() != null && !oldMusic.getCoverUrl().isEmpty()) ossService.deleteFile(oldMusic.getCoverUrl());
        }

        if (newAudio != null && !newAudio.isEmpty()) {
            String ext = newAudio.getOriginalFilename().substring(newAudio.getOriginalFilename().lastIndexOf("."));
            form.setAudioUrl(ossService.uploadFileWithCustomName(newAudio, "songs/", safeBaseName + ext));
            if (oldMusic.getAudioUrl() != null && !oldMusic.getAudioUrl().isEmpty()) ossService.deleteFile(oldMusic.getAudioUrl());
        }

        if (newLyric != null && !newLyric.isEmpty()) {
            form.setLyricUrl(ossService.uploadFileWithCustomName(newLyric, "lyrics/", safeBaseName + ".lrc"));
            if (oldMusic.getLyricUrl() != null && !oldMusic.getLyricUrl().isEmpty()) ossService.deleteFile(oldMusic.getLyricUrl());
        }

        musicService.updateMusic(form);
        return Result.success("歌曲信息更新成功！");
    }
}