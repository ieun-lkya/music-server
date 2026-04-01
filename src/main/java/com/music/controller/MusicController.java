package com.music.controller;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.music.common.Result;
import com.music.entity.MusicInfo;
import com.music.service.MusicService;
import lombok.extern.slf4j.Slf4j; // 引入日志注解
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@Slf4j // 加上这个，就可以直接用 log.info() 打印专业日志了！
@RestController
@RequestMapping("/music")
public class MusicController {

    @Autowired
    private MusicService musicService;

    @GetMapping("/list")
    public Result<List<MusicInfo>> getMusicList() {
        return Result.success(musicService.getAllMusic());
    }

    /**
     * 分页获取音乐列表
     * @param pageNum 页码，默认第 1 页
     * @param pageSize 每页条数，默认 10 条
     * @return 分页结果，包含音乐列表和总记录数等信息
     */
    @GetMapping("/page")
    public Result<PageInfo<MusicInfo>> getMusicPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        // 核心：开启物理分页！它会自动帮你拼接 LIMIT ?, ?
        PageHelper.startPage(pageNum, pageSize);
        List<MusicInfo> list = musicService.getAllMusic();
        
        // 包装成大厂标准的 PageInfo 返回（里面包含了总条数 total 等参数）
        return Result.success(new PageInfo<>(list));
    }

    @PostMapping("/recommend")
    public Result<List<MusicInfo>> recommend(@RequestBody Map<String, String> params) {
        String sceneText = params.get("text");
        // 【关键日志 1】看看前端到底传过来了什么
        log.info("====== 1. [Controller] 收到前端发来的场景文本: [{}] ======", sceneText);

        List<MusicInfo> list = musicService.recommendMusic(sceneText);

        // 【关键日志 4】看看最后准备返回多少条数据
        log.info("====== 4. [Controller] 准备将 {} 条匹配结果返回给前端 ======", list.size());
        return Result.success(list);
    }
    @PostMapping("/add")
    public Result<String> addMusic(@RequestBody MusicInfo musicInfo) {
        log.info("====== [Controller] 接收到入库请求: {} ======", musicInfo.getTitle());
        musicService.addMusic(musicInfo);
        return Result.success("歌曲发布成功！");
    }
    @DeleteMapping("/delete/{id}")
    public Result<String> deleteMusic(@PathVariable Long id) {
        musicService.deleteMusic(id);
        return Result.success("删除成功！");
    }

    @PostMapping("/updateCover")
    public Result<String> updateCover(@RequestParam("id") Long id, @RequestParam("coverUrl") String coverUrl) {
        musicService.updateCover(id, coverUrl);
        return Result.success("封面更新成功！");
    }
}