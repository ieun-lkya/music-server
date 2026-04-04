create table music_comment
(
    id          int auto_increment
        primary key,
    music_id    int                                not null comment '歌曲ID',
    user_id     int                                not null comment '评论用户ID',
    content     varchar(500)                       not null comment '评论内容',
    likes       int      default 0                 null comment '点赞数',
    create_time datetime default CURRENT_TIMESTAMP null comment '评论时间',
    parent_id   int                                null comment '父评论ID'
)
    comment '云村热评表' charset = utf8mb4;

create table music_info
(
    id          int auto_increment comment '主键ID'
        primary key,
    title       varchar(100)                       not null comment '歌曲名称',
    artist      varchar(100)                       not null comment '歌手名称',
    cover_url   varchar(500)                       null comment '封面图URL',
    audio_url   varchar(500)                       null comment '音频文件URL',
    tags        varchar(200)                       null comment '场景标签(如: 安静,深夜,学习)',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    lyric_url   varchar(255)                       null comment '本地歌词地址',
    play_count  int      default 0                 null comment '播放量'
)
    comment '音乐曲库表';

create table playlist
(
    id          bigint auto_increment
        primary key,
    user_id     bigint                             not null comment '所属用户',
    name        varchar(50)                        not null comment '歌单名称',
    create_time datetime default CURRENT_TIMESTAMP null
)
    charset = utf8mb4;

create table playlist_music
(
    playlist_id bigint                             not null,
    music_id    bigint                             not null,
    create_time datetime default CURRENT_TIMESTAMP null,
    primary key (playlist_id, music_id)
)
    charset = utf8mb4;

create table user_collected_playlist
(
    id          int auto_increment
        primary key,
    user_id     int                                not null,
    playlist_id int                                not null,
    create_time datetime default CURRENT_TIMESTAMP null,
    constraint idx_user_playlist
        unique (user_id, playlist_id)
)
    charset = utf8mb4;

create table user_info
(
    id          bigint auto_increment comment '用户主键'
        primary key,
    username    varchar(50)                                                                                not null comment '登录账号',
    password    varchar(100)                                                                               not null comment '登录密码',
    avatar      varchar(255) default 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png' null comment '用户头像',
    create_time datetime     default CURRENT_TIMESTAMP                                                     null comment '注册时间',
    signature   varchar(255) default '用音乐记录生活，寻找灵魂共鸣...'                                      null,
    constraint uk_username
        unique (username)
)
    comment '用户表' charset = utf8mb4;

create table user_likes
(
    id          bigint auto_increment comment '主键'
        primary key,
    user_id     bigint                             not null comment '谁收藏的',
    music_id    bigint                             not null comment '收藏了哪首歌',
    create_time datetime default CURRENT_TIMESTAMP null comment '收藏时间',
    constraint uk_user_music
        unique (user_id, music_id)
)
    comment '用户收藏关系表' charset = utf8mb4;

create table user_play_log
(
    id          int auto_increment
        primary key,
    user_id     int                                not null,
    music_id    int                                not null,
    create_time datetime default CURRENT_TIMESTAMP null
)
    charset = utf8mb4;

create index idx_user_time
    on user_play_log (user_id, create_time);

