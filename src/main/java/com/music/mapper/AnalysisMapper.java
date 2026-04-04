package com.music.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface AnalysisMapper {
    //  核心修改：把 84 DAY 改成 28 DAY (4周)
    @Select("SELECT DATE_FORMAT(create_time, '%Y/%m/%d') as date, COUNT(*) as count " +
            "FROM user_play_log " +
            "WHERE user_id = #{userId} AND create_time >= DATE_SUB(CURDATE(), INTERVAL 28 DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y/%m/%d')")
    List<Map<String, Object>> getHeatmapData(Integer userId);

    //  核心修复 SQL 2：表名改为 music_info，字段名改为 tags！！！
    @Select("SELECT m.tags as name, COUNT(*) as value " +
            "FROM user_play_log l JOIN music_info m ON l.music_id = m.id " +
            "WHERE l.user_id = #{userId} AND m.tags IS NOT NULL AND m.tags != '' " +
            "GROUP BY m.tags LIMIT 6")
    List<Map<String, Object>> getRadarData(Integer userId);
    
    //  核心 SQL 3：插入一条听歌流水
    @Insert("INSERT INTO user_play_log(user_id, music_id) VALUES(#{userId}, #{musicId})")
    void insertPlayLog(@Param("userId") Integer userId, @Param("musicId") Integer musicId);
}
