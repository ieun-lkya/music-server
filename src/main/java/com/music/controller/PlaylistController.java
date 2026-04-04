package com.music.controller;

import com.music.common.Result;
import com.music.mapper.PlaylistMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/playlist")
public class PlaylistController {

    //  完美注入！抛弃 JdbcTemplate！
    @Autowired
    private PlaylistMapper playlistMapper;

    @PostMapping("/create")
    public Result<String> createPlaylist(@RequestParam Long userId, @RequestParam String name) {
        playlistMapper.createPlaylist(userId, name);
        return Result.success("歌单创建成功");
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getUserPlaylists(@RequestParam Long userId) {
        return Result.success(playlistMapper.getUserPlaylists(userId));
    }

    @PostMapping("/addMusic")
    public Result<String> addMusicToPlaylist(@RequestParam Long playlistId, @RequestParam Long musicId) {
        playlistMapper.addMusicToPlaylist(playlistId, musicId);
        return Result.success("已添加到云端歌单");
    }

    @GetMapping("/musicList")
    public Result<List<Map<String, Object>>> getPlaylistMusic(@RequestParam Long playlistId) {
        return Result.success(playlistMapper.getPlaylistMusic(playlistId));
    }

    @DeleteMapping("/delete")
    public Result<String> deletePlaylist(@RequestParam Long playlistId) {
        playlistMapper.deletePlaylistMusic(playlistId); // 先删关联
        playlistMapper.deletePlaylist(playlistId);      // 再删本体
        return Result.success("歌单已删除");
    }

    @GetMapping("/all")
    public Result<List<Map<String, Object>>> getAllPlaylists() {
        return Result.success(playlistMapper.getAllPlaylists());
    }

    @PostMapping("/collect")
    public Result<String> collectPlaylist(@RequestParam Integer userId, @RequestParam Integer playlistId) {
        playlistMapper.collectPlaylist(userId, playlistId);
        return Result.success("收藏成功");
    }

    @PostMapping("/uncollect")
    public Result<String> uncollectPlaylist(@RequestParam Integer userId, @RequestParam Integer playlistId) {
        playlistMapper.uncollectPlaylist(userId, playlistId);
        return Result.success("取消收藏");
    }

    @GetMapping("/collected/{userId}")
    public Result<List<Map<String, Object>>> getCollectedPlaylists(@PathVariable Integer userId) {
        return Result.success(playlistMapper.getCollectedPlaylists(userId));
    }
}
