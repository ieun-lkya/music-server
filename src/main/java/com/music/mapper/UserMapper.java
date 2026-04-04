package com.music.mapper;

import com.music.entity.MusicInfo;
import com.music.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface UserMapper {

    // 1. 根据用户名查找用户 (用于登录校验和防重复注册)
    @Select("SELECT * FROM user_info WHERE username = #{username}")
    User findByUsername(String username);

    // 2. 注册新用户
    @Insert("INSERT INTO user_info(username, password, avatar) VALUES(#{username}, #{password}, #{avatar})")
    void insertUser(User user);

    // 3. 收藏歌曲 (忽略重复冲突)
    @Insert("INSERT IGNORE INTO user_likes(user_id, music_id) VALUES(#{userId}, #{musicId})")
    void likeMusic(@Param("userId") Long userId, @Param("musicId") Long musicId);

    // 4. 取消收藏
    @Delete("DELETE FROM user_likes WHERE user_id = #{userId} AND music_id = #{musicId}")
    void unlikeMusic(@Param("userId") Long userId, @Param("musicId") Long musicId);

    // 5. 联合查询：获取某个用户收藏的所有歌曲！(真正的多表联查)
    @Select("SELECT m.* FROM music_info m " +
            "INNER JOIN user_likes ul ON m.id = ul.music_id " +
            "WHERE ul.user_id = #{userId} " +
            "ORDER BY ul.create_time DESC")
    List<MusicInfo> getLikedMusicList(Long userId);

    // 6. 🚀 统计用户总数 (用于 Admin 大屏)
    @Select("SELECT COUNT(*) FROM user_info")
    long countUser();

    // 7. 核心：允许用户更新自己的名片
    @org.apache.ibatis.annotations.Update("UPDATE user_info SET username = #{username}, avatar = #{avatar}, signature = #{signature} WHERE id = #{id}")
    void updateUser(com.music.entity.User user);

    // 8. 顺手加个按 ID 查询，用来返回最新数据
    @org.apache.ibatis.annotations.Select("SELECT * FROM user_info WHERE id = #{id}")
    com.music.entity.User selectById(Integer id);
}