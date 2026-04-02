package com.music.mapper;

import com.music.entity.Comment;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommentMapper {
    
    @Insert("INSERT INTO music_comment(music_id, user_id, content, parent_id, create_time) " +
            "VALUES(#{musicId}, #{userId}, #{content}, #{parentId}, NOW())")
    void insert(Comment comment);

    //  修正了极其致命的表名错误：将 user 改为真实的表名 user_info！
    @Select("SELECT c.*, u.username, u.avatar FROM music_comment c " +
            "LEFT JOIN user_info u ON c.user_id = u.id " +
            "WHERE c.music_id = #{musicId} " +
            "ORDER BY c.likes DESC, c.create_time DESC")
    List<Comment> selectByMusicId(Integer musicId);

    //  给热评注入灵魂：点赞数 +1
    @org.apache.ibatis.annotations.Update("UPDATE music_comment SET likes = likes + 1 WHERE id = #{id}")
    void incrementLikes(Integer id);
}
