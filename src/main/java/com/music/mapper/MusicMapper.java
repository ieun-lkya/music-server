package com.music.mapper;

import com.music.entity.MusicInfo;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MusicMapper {

    // 1. 查询全部
    @Select("SELECT * FROM music_info ORDER BY create_time DESC")
    List<MusicInfo> selectAll();

    // 2. 🚀 核心新增：插入时带上 lyric_url
    @Insert("INSERT INTO music_info(title, artist, cover_url, audio_url, lyric_url, tags) " +
            "VALUES(#{title}, #{artist}, #{coverUrl}, #{audioUrl}, #{lyricUrl}, #{tags})")
    void insertMusic(MusicInfo musicInfo);

    // 3. AI 标签模糊匹配搜索
    @Select("SELECT * FROM music_info WHERE tags LIKE CONCAT('%', #{tag}, '%') ORDER BY create_time DESC")
    List<MusicInfo> searchByTag(String tag);

    // 4. 根据 ID 查询 (清理了重复的 findById)
    @Select("SELECT * FROM music_info WHERE id = #{id}")
    MusicInfo getById(Long id);

    // 5. 物理删除 (清理了重复的 deleteMusicById)
    @Delete("DELETE FROM music_info WHERE id = #{id}")
    void deleteById(Long id);

    // 6. 🚀 动态更新 (带上 lyricUrl 的更新逻辑)
    @Update("<script>" +
            "UPDATE music_info SET title=#{title}, artist=#{artist}, tags=#{tags} " +
            "<if test='coverUrl != null'>, cover_url=#{coverUrl}</if> " +
            "<if test='audioUrl != null'>, audio_url=#{audioUrl}</if> " +
            "<if test='lyricUrl != null'>, lyric_url=#{lyricUrl}</if> " +
            "WHERE id=#{id}" +
            "</script>")
    void updateMusic(MusicInfo music);
}