package com.example.sis.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class VimeoService {
    
    private static final Logger logger = LoggerFactory.getLogger(VimeoService.class);
    
    @Value("${vimeo.access-token:}")
    private String accessToken;
    
    @Value("${vimeo.api-url:https://api.vimeo.com/videos}")
    private String apiUrl;
    
    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Extract video ID từ Vimeo URL
     * Hỗ trợ:
     * - https://vimeo.com/1139910461
     * - https://player.vimeo.com/video/1139910461
     * - https://vimeo.com/1139910461?share=copy&fl=ip&fe=ec
     */
    public String extractVideoId(String vimeoUrl) {
        if (vimeoUrl == null || vimeoUrl.isEmpty()) {
            throw new IllegalArgumentException("Vimeo URL is required");
        }
        
        Pattern pattern = Pattern.compile("vimeo\\.com/(?:video/)?(\\d+)");
        Matcher matcher = pattern.matcher(vimeoUrl);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        throw new IllegalArgumentException("Invalid Vimeo URL format: " + vimeoUrl);
    }
    
    /**
     * Lấy duration từ Vimeo API (trả về phút)
     */
    public Integer getVideoDuration(String vimeoUrl) {
        try {
            String videoId = extractVideoId(vimeoUrl);
            logger.info("Fetching duration for Vimeo video ID: {}", videoId);
            
            // Nếu không có access token, return null
            if (accessToken == null || accessToken.isEmpty()) {
                logger.warn("Vimeo access token not configured");
                return null;
            }
            
            Request request = new Request.Builder()
                    .url(apiUrl + "/" + videoId)
                    .addHeader("Authorization", "Bearer " + accessToken)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    logger.error("Vimeo API error: HTTP {}", response.code());
                    return null;
                }
                
                String responseBody = response.body().string();
                JsonNode jsonNode = objectMapper.readTree(responseBody);
                
                // Duration in seconds
                int durationSeconds = jsonNode.get("duration").asInt();
                int durationMinutes = (int) Math.ceil(durationSeconds / 60.0);
                
                logger.info("Video duration: {} seconds ({} minutes)", durationSeconds, durationMinutes);
                return durationMinutes;
            }
            
        } catch (Exception e) {
            logger.error("Error fetching Vimeo video duration: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert Vimeo URL to embed URL
     */
    public String toEmbedUrl(String vimeoUrl) {
        String videoId = extractVideoId(vimeoUrl);
        return "https://player.vimeo.com/video/" + videoId;
    }
}
