package com.music.controller;

import com.music.common.Result;
import com.music.mapper.AnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {
    
    @Autowired
    private AnalysisMapper analysisMapper;

    @GetMapping("/heatmap/{userId}")
    public Result<List<Map<String, Object>>> getHeatmap(@PathVariable Integer userId) {
        return Result.success(analysisMapper.getHeatmapData(userId));
    }

    @GetMapping("/radar/{userId}")
    public Result<List<Map<String, Object>>> getRadar(@PathVariable Integer userId) {
        return Result.success(analysisMapper.getRadarData(userId));
    }
    
    @PostMapping("/record")
    public Result<String> recordPlay(@RequestParam Integer userId, @RequestParam Integer musicId) {
        analysisMapper.insertPlayLog(userId, musicId);
        return Result.success("流水记录成功！");
    }
}
