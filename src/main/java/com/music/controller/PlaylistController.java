package com.music.controller;

import com.music.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 云端歌单控制器 - "音乐收藏夹"
 * 提供用户创建、管理个人歌单的功能
 */
@RestController
@RequestMapping("/playlist")
public class PlaylistController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 创建个人歌单
     * @param userId 用户 ID
     * @param name 歌单名称
     * @return 操作结果
     */
    @PostMapping("/create")
    public Result<String> createPlaylist(@RequestParam Long userId, @RequestParam String name) {
        jdbcTemplate.update("INSERT INTO playlist(user_id, name) VALUES(?, ?)", userId, name);
        return Result.success("歌单创建成功");
    }

    /**
     * 获取用户的所有歌单列表
     * @param userId 用户 ID
     * @return 用户的歌单列表
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getUserPlaylists(@RequestParam Long userId) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "SELECT * FROM playlist WHERE user_id = ? ORDER BY create_time DESC", 
            userId
        );
        return Result.success(list);
    }

    /**
     * 添加歌曲到歌单
     * @param playlistId 歌单 ID
     * @param musicId 音乐 ID
     * @return 操作结果
     */
    @PostMapping("/addMusic")
    public Result<String> addMusicToPlaylist(@RequestParam Long playlistId, @RequestParam Long musicId) {
        // INSERT IGNORE 避免重复添加
        jdbcTemplate.update(
            "INSERT IGNORE INTO playlist_music(playlist_id, music_id) VALUES(?, ?)", 
            playlistId, musicId
        );
        return Result.success("已添加到云端歌单");
    }

    /**
     * 获取歌单中的所有音乐
     * @param playlistId 歌单 ID
     * @return 歌单中的音乐列表
     */
    @GetMapping("/musicList")
    public Result<List<Map<String, Object>>> getPlaylistMusic(@RequestParam Long playlistId) {
        String sql = "SELECT m.* FROM music_info m " +
                     "INNER JOIN playlist_music pm ON m.id = pm.music_id " +
                     "WHERE pm.playlist_id = ? " +
                     "ORDER BY pm.create_time DESC";
        return Result.success(jdbcTemplate.queryForList(sql, playlistId));
    }

    /**
     * 删除歌单
     * @param playlistId 歌单 ID
     * @return 操作结果
     */
    @DeleteMapping("/delete")
    public Result<String> deletePlaylist(@RequestParam Long playlistId) {
        // 先删除关联表数据，再删除歌单本身
        jdbcTemplate.update("DELETE FROM playlist_music WHERE playlist_id = ?", playlistId);
        jdbcTemplate.update("DELETE FROM playlist WHERE id = ?", playlistId);
        return Result.success("歌单已删除");
    }

    /**
     * 核弹级接口：拉取全站所有用户的歌单，组装歌单广场！
     * @return 所有歌单列表（包含创建者信息）
     */
    @GetMapping("/all")
    public Result<List<Map<String, Object>>> getAllPlaylists() {
        // 直接连表查询，把歌单和它的创建者名字一并抓出来！
        String sql = "SELECT p.*, u.username as creatorName FROM playlist p " +
                     "LEFT JOIN user_info u ON p.user_id = u.id ORDER BY p.id DESC";
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql);
        return Result.success(list);
    }
}
