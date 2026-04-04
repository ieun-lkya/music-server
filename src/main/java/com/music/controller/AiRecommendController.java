package com.music.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/music/ai")
public class AiRecommendController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    //  核心业务 1：智能电台 & 场景匹配 (精准定位、解析上下文) 【被我误删的代码已完美复活】
    @GetMapping("/recommend")
    public Result<List<Map<String, Object>>> recommend(@RequestParam String scene) {
        String API_KEY = System.getenv("DASHSCOPE_API_KEY");
        if (API_KEY == null || API_KEY.isEmpty()) {
            log.error("致命错误：系统环境变量 DASHSCOPE_API_KEY 未配置！");
            return Result.error("AI 引擎未授权，请配置 DASHSCOPE_API_KEY");
        }
        
        try {
            List<Map<String, Object>> allMusicInfo = jdbcTemplate.queryForList("SELECT id, title, artist FROM music_info");
            if (allMusicInfo.isEmpty()) return Result.error("曲库空空如也，快去后台发布音乐吧！");

            StringBuilder musicCatalog = new StringBuilder();
            for (Map<String, Object> m : allMusicInfo) {
                musicCatalog.append(m.get("id")).append(". 《").append(m.get("title")).append("》 - ").append(m.get("artist")).append("\n");
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(API_KEY);

            String prompt = String.format(
                    "你是一个极其智能、富有同理心的首席音乐DJ。用户的搜索需求是：【%s】\n" +
                            "请仔细阅读下方的【专属曲库菜单】（每行前面是歌曲 ID）：\n%s\n" +
                            "【点歌规则与思考路径 - 极其重要】：\n" +
                            "1. 若用户【明确搜索】某首歌名或歌手：请直接精确匹配。如果库里没有，必须判定为 NONE。\n" +
                            "2. 若用户输入的是【模糊场景/心情】：请先用几句话深度剖析用户当前的心境和潜在的情感诉求，然后在脑海中与菜单中的歌曲逐一比对，挑选出 1 到 8 首歌。\n" +
                            "3. 格式要求：你可以尽情输出你的分析和思考过程，但在你思考结束的最后，你**必须**将最终选出的歌曲 ID 放在 <result> 和 </result> 标签内，用英文逗号分隔。\n" +
                            "【极其标准的输出示范】：\n" +
                            "用户说他失恋了，此时他可能感到沮丧和孤独。我看到曲库中第3首和第7首是伤感慢歌，非常契合他的情绪...\n" +
                            "<result>3,7</result>\n" +
                            "（注意：如果没有合适的歌，请在标签内填入 NONE，即 <result>NONE</result>）",
                    scene, musicCatalog.toString()
            );

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", "qwen-plus");

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", prompt);
            messages.add(systemMsg);

            requestBodyMap.put("messages", messages);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            String aiResponse = root.path("choices").get(0).path("message").path("content").asText().trim();
            log.info("AI 智能电台深度思考过程:\n{}", aiResponse);

            String idContent = "";
            Pattern pattern = Pattern.compile("<result>([\\s\\S]*?)</result>", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(aiResponse);
            if (matcher.find()) idContent = matcher.group(1).trim();
            else idContent = aiResponse;

            if (idContent.toUpperCase().contains("NONE")) return Result.error(" 抱歉，曲库中暂未收录合适的歌曲，请尝试换个词或搜歌手~");

            String[] idStrings = idContent.replaceAll("[^0-9,]", "").split(",");
            List<Integer> validIds = new ArrayList<>();
            for (String idStr : idStrings) {
                if (!idStr.isEmpty()) {
                    try { validIds.add(Integer.parseInt(idStr)); } catch (NumberFormatException ignored) {}
                }
            }
            if (validIds.isEmpty()) return Result.error(" AI 找不到合适的歌曲，请换个场景描述吧~");

            String inSql = String.join(",", Collections.nCopies(validIds.size(), "?"));
            String finalSql = String.format("SELECT id, title, artist, cover_url AS coverUrl, audio_url AS audioUrl, lyric_url AS lyricUrl FROM music_info WHERE id IN (%s)", inSql);
            List<Map<String, Object>> recommendedMusic = jdbcTemplate.queryForList(finalSql, validIds.toArray());
            return Result.success(recommendedMusic);

        } catch (Exception e) {
            log.error("AI 推荐接口发生代码级异常", e);
            return Result.error("AI 大脑暂时走神了，请稍后再试");
        }
    }

    //  核心业务 2：广场量子坍缩歌单 (零干预、高熵值、JSON结构化输出) 【全新超神引擎】
    @GetMapping("/generatePlaylists")
    public Result<List<Map<String, Object>>> generatePlaylists() {
        String API_KEY = System.getenv("DASHSCOPE_API_KEY");
        if (API_KEY == null || API_KEY.isEmpty()) return Result.error("请配置环境变量 DASHSCOPE_API_KEY");

        try {
            List<Map<String, Object>> allMusicInfo = jdbcTemplate.queryForList("SELECT id, title, artist FROM music_info");
            if (allMusicInfo.isEmpty()) return Result.error("曲库空空如也");

            StringBuilder musicCatalog = new StringBuilder();
            for (Map<String, Object> m : allMusicInfo) {
                musicCatalog.append(m.get("id")).append(". 《").append(m.get("title")).append("》 - ").append(m.get("artist")).append("\n");
            }

            //  核心机制：注入极其抽象的随机时空指纹，强制打碎 LLM 的缓存倾向
            String timeFingerprint = UUID.randomUUID().toString() + "-" + System.nanoTime();

            //  终极 Prompt：禁止任何限制，要求深度逻辑输出
            String prompt = String.format(
                    "你是一个极具灵魂深度和音乐审美的AI主理人。当前随机时空指纹为：【%s】。\n" +
                    "请阅读下方的【曲库全集】：\n%s\n" +
                    "【你的使命】：\n" +
                    "1. 请完全摆脱任何已知的音乐分类模板（如赛博、治愈等陈词滥调），利用你庞大的知识图谱，**自发创造** 4 个独特的歌单主题。\n" +
                    "2. 为每个歌单撰写一个【分类依据(basis)】：深层剖析这几首歌在编曲、歌词意境、或者某种抽象情感上的内在联系，告诉听众你为什么这样分类（不少于40字）。\n" +
                    "3. 挑选 3-6 首歌曲 ID 放入 ids 数组。\n" +
                    "【严格格式】：必须在 <result> 标签内返回纯净 JSON 数组，包含 name, description, tags, basis, ids 字段。",
                    timeFingerprint, musicCatalog.toString()
            );

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(API_KEY);

            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", "qwen-plus");
            requestBodyMap.put("temperature", 1.0); //  满额创造力，杜绝重复！
            requestBodyMap.put("top_p", 0.9);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", prompt);
            messages.add(systemMsg);
            requestBodyMap.put("messages", messages);

            ObjectMapper mapper = new ObjectMapper();
            String jsonBody = mapper.writeValueAsString(requestBodyMap);
            HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(API_URL, entity, String.class);

            JsonNode root = mapper.readTree(response.getBody());
            String aiResponse = root.path("choices").get(0).path("message").path("content").asText().trim();
            log.info("AI 量子歌单生成结果:\n{}", aiResponse);

            Matcher matcher = Pattern.compile("<result>([\\s\\S]*?)</result>", Pattern.CASE_INSENSITIVE).matcher(aiResponse);
            String jsonContent = matcher.find() ? matcher.group(1).trim() : aiResponse;
            jsonContent = jsonContent.replaceAll("```json", "").replaceAll("```", "").trim();

            JsonNode playlistsNode = mapper.readTree(jsonContent);
            List<Map<String, Object>> finalPlaylists = new ArrayList<>();
            
            if (playlistsNode.isArray()) {
                for (JsonNode node : playlistsNode) {
                    List<Integer> validIds = new ArrayList<>();
                    if (node.path("ids").isArray()) {
                        for (JsonNode idNode : node.path("ids")) validIds.add(idNode.asInt());
                    }
                    if (validIds.isEmpty()) continue;

                    String inSql = String.join(",", Collections.nCopies(validIds.size(), "?"));
                    String sql = String.format("SELECT id, title, artist, cover_url AS coverUrl, audio_url AS audioUrl, lyric_url AS lyricUrl FROM music_info WHERE id IN (%s)", inSql);
                    List<Map<String, Object>> songs = jdbcTemplate.queryForList(sql, validIds.toArray());
                    
                    if (!songs.isEmpty()) {
                        Map<String, Object> pl = new HashMap<>();
                        pl.put("id", "ai_" + System.currentTimeMillis() + "_" + Math.random());
                        pl.put("name", node.path("name").asText());
                        pl.put("description", node.path("description").asText());
                        pl.put("basis", node.path("basis").asText()); //  注入分类依据！
                        pl.put("tags", node.path("tags").asText());
                        pl.put("creatorName", "Echo AI 引擎");
                        pl.put("isAi", true);
                        pl.put("songs", songs);
                        pl.put("coverUrl", songs.get(0).get("coverUrl"));
                        finalPlaylists.add(pl);
                    }
                }
            }
            return Result.success(finalPlaylists);
        } catch (Exception e) {
            log.error("AI 歌单生成异常", e);
            return Result.error("AI 引擎正在星际漫游，请稍后再试");
        }
    }
}