package com.music.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface PlaylistMapper {

    // 1. 创建个人歌单
    @Insert("INSERT INTO playlist(user_id, name) VALUES(#{userId}, #{name})")
    void createPlaylist(@Param("userId") Long userId, @Param("name") String name);

    // 2. 获取用户的所有歌单列表
    @Select("SELECT * FROM playlist WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Map<String, Object>> getUserPlaylists(@Param("userId") Long userId);

    // 3. 添加歌曲到歌单 (忽略重复)
    @Insert("INSERT IGNORE INTO playlist_music(playlist_id, music_id) VALUES(#{playlistId}, #{musicId})")
    void addMusicToPlaylist(@Param("playlistId") Long playlistId, @Param("musicId") Long musicId);

    // 4. 获取歌单中的所有音乐
    @Select("SELECT m.* FROM music_info m " +
            "INNER JOIN playlist_music pm ON m.id = pm.music_id " +
            "WHERE pm.playlist_id = #{playlistId} " +
            "ORDER BY pm.create_time DESC")
    List<Map<String, Object>> getPlaylistMusic(@Param("playlistId") Long playlistId);

    // 5. 删除歌单关联的歌曲记录
    @Delete("DELETE FROM playlist_music WHERE playlist_id = #{playlistId}")
    void deletePlaylistMusic(@Param("playlistId") Long playlistId);

    // 6. 删除歌单本身
    @Delete("DELETE FROM playlist WHERE id = #{playlistId}")
    void deletePlaylist(@Param("playlistId") Long playlistId);

    // 7.  拉取全站歌单广场（连带提取第一首歌封面）
    @Select("SELECT p.*, u.username as creatorName, " +
            "(SELECT m.cover_url FROM playlist_music pm JOIN music_info m ON pm.music_id = m.id WHERE pm.playlist_id = p.id LIMIT 1) as coverUrl " +
            "FROM playlist p " +
            "LEFT JOIN user_info u ON p.user_id = u.id " +
            "ORDER BY p.id DESC")
    List<Map<String, Object>> getAllPlaylists();

    // 8.  收藏歌单
    @Insert("INSERT IGNORE INTO user_collected_playlist(user_id, playlist_id) VALUES(#{userId}, #{playlistId})")
    void collectPlaylist(@Param("userId") Integer userId, @Param("playlistId") Integer playlistId);

    // 9.  取消收藏歌单
    @Delete("DELETE FROM user_collected_playlist WHERE user_id = #{userId} AND playlist_id = #{playlistId}")
    void uncollectPlaylist(@Param("userId") Integer userId, @Param("playlistId") Integer playlistId);

    // 10.  获取我的收藏歌单（连带提取封面）
    @Select("SELECT p.*, " +
            "(SELECT m.cover_url FROM playlist_music pm JOIN music_info m ON pm.music_id = m.id WHERE pm.playlist_id = p.id LIMIT 1) as coverUrl " +
            "FROM user_collected_playlist ucp " +
            "JOIN playlist p ON ucp.playlist_id = p.id " +
            "WHERE ucp.user_id = #{userId}")
    List<Map<String, Object>> getCollectedPlaylists(Integer userId);
}
