package com.music.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {
    //  核心 SQL 1：按天聚合过去 84 天的听歌数量 (热力图)
    @Select("SELECT DATE_FORMAT(create_time, '%Y/%m/%d') as date, COUNT(*) as count " +
            "FROM user_play_log " +
            "WHERE user_id = #{userId} AND create_time >= DATE_SUB(CURDATE(), INTERVAL 84 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y/%m/%d')")
    List<Map<String, Object>> getHeatmapData(Integer userId);

    //  核心 SQL 2：联表查询统计用户的偏好标签 (雷达图)
    @Select("SELECT m.style_tag as name, COUNT(*) as value " +
            "FROM user_play_log l JOIN music m ON l.music_id = m.id " +
            "WHERE l.user_id = #{userId} " +
            "GROUP BY m.style_tag LIMIT 6")
    List<Map<String, Object>> getRadarData(Integer userId);
    
    //  核心 SQL 3：插入一条听歌流水
    @Insert("INSERT INTO user_play_log(user_id, music_id) VALUES(#{userId}, #{musicId})")
    void insertPlayLog(@Param("userId") Integer userId, @Param("musicId") Integer musicId);
}
