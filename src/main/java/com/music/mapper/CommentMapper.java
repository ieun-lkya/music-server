package com.music.mapper;

import com.music.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CommentMapper {
    
    @Insert("INSERT INTO music_comment(music_id, user_id, content, create_time) " +
            "VALUES(#{musicId}, #{userId}, #{content}, NOW())")
    void insert(Comment comment);

    //  极其精妙的联表查询：按点赞数和时间降序排列，这就是"热评"的核心算法！
    @Select("SELECT c.*, u.username, u.avatar FROM music_comment c " +
            "LEFT JOIN user u ON c.user_id = u.id " +
            "WHERE c.music_id = #{musicId} " +
            "ORDER BY c.likes DESC, c.create_time DESC")
    List<Comment> selectByMusicId(Integer musicId);
}
