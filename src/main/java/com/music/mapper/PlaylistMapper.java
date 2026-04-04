package com.music.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface PlaylistMapper {

    //  核心替换：在获取所有歌单时，利用子查询强行拉取第一首歌的 cover_url 作为歌单封面！
    @Select("SELECT p.*, u.username as creatorName, " +
            "(SELECT m.cover_url FROM playlist_music pm JOIN music_info m ON pm.music_id = m.id WHERE pm.playlist_id = p.id LIMIT 1) as coverUrl " +
            "FROM playlist p JOIN user u ON p.creator_id = u.id")
    List<Map<String, Object>> getAllPlaylists();

    //  新增：收藏表操作
    @Insert("INSERT IGNORE INTO user_collected_playlist(user_id, playlist_id) VALUES(#{userId}, #{playlistId})")
    void collectPlaylist(@Param("userId") Integer userId, @Param("playlistId") Integer playlistId);

    @Delete("DELETE FROM user_collected_playlist WHERE user_id = #{userId} AND playlist_id = #{playlistId}")
    void uncollectPlaylist(@Param("userId") Integer userId, @Param("playlistId") Integer playlistId);

    //  新增：获取我的收藏歌单（同样附带抽取第一首歌当封面）
    @Select("SELECT p.*, " +
            "(SELECT m.cover_url FROM playlist_music pm JOIN music_info m ON pm.music_id = m.id WHERE pm.playlist_id = p.id LIMIT 1) as coverUrl " +
            "FROM user_collected_playlist ucp " +
            "JOIN playlist p ON ucp.playlist_id = p.id " +
            "WHERE ucp.user_id = #{userId}")
    List<Map<String, Object>> getCollectedPlaylists(Integer userId);
}
