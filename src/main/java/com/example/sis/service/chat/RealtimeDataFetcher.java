package com.example.sis.service.chat;

import com.example.sis.service.system.SystemStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to detect when user asks questions about real-time system data
 * and fetch that data from internal APIs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeDataFetcher {
    
    private final SystemStatsService systemStatsService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    
    @Value("${server.port:7000}")
    private int serverPort;
    
    /**
     * Analyze user message and determine if it requires real-time data
     */
    public boolean needsRealtimeData(String userMessage) {
        String msg = userMessage.toLowerCase();
        
        // Patterns that indicate need for real-time data
        return msg.contains("bao nhiêu") ||
               msg.contains("thống kê") ||
               msg.contains("hiện tại") ||
               msg.contains("hôm nay") ||
               msg.contains("bao giờ") ||
               msg.contains("khi nào") ||
               msg.contains("còn") ||
               msg.contains("đang có") ||
               msg.contains("tổng số") ||
               msg.contains("số lượng");
    }
    
    /**
     * Fetch real-time data based on user question and inject into context
     */
    public String buildContextWithRealtimeData(String userMessage, String baseContext) {
        log.info("🔍 RealtimeDataFetcher called with message: {}", userMessage);
        
        // Check if real-time data is needed
        if (!needsRealtimeData(userMessage)) {
            log.info("❌ No real-time data keywords detected, using RAG only");
            return baseContext;
        }
        
        log.info("✅ Real-time data keywords detected!");
        
        StringBuilder dataContext = new StringBuilder();
        
        // Get current date in Vietnam timezone
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        
        String msg = userMessage.toLowerCase();
        boolean hasRealtimeData = false;
        
        try {
            // Question about current date
            if ((msg.contains("hôm nay") || msg.contains("ngày hôm nay") || msg.contains("bây giờ")) && 
                (msg.contains("ngày") || msg.contains("tháng") || msg.contains("năm") || msg.contains("bao nhiêu"))) {
                log.info("✅ Detected current date question");
                if (!hasRealtimeData) {
                    dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                    dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n", today.toString()));
                    dataContext.append("⚠️ BẮT BUỘC dùng ngày này làm 'hôm nay', KHÔNG ĐƯỢC dùng ngày khác!\n\n");
                    hasRealtimeData = true;
                }
                dataContext.append(String.format("- Ngày hiện tại (hôm nay): %s (%s/%s/%s)\n", 
                    today.toString(), 
                    String.format("%02d", today.getDayOfMonth()),
                    String.format("%02d", today.getMonthValue()),
                    today.getYear()));
            }
            
            // Question about total users
            if (msg.contains("bao nhiêu user") || msg.contains("bao nhiêu người dùng") || 
                (msg.contains("user") && msg.contains("bao nhiêu"))) {
                log.info("✅ Detected user count question");
                Long count = systemStatsService.getTotalUsers();
                if (!hasRealtimeData) {
                    dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                    dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n", today.toString()));
                    dataContext.append("⚠️ BẮT BUỘC dùng ngày này làm 'hôm nay', KHÔNG ĐƯỢC dùng ngày khác!\n\n");
                    hasRealtimeData = true;
                }
                dataContext.append(String.format("- Tổng số người dùng HIỆN TẠI: %d người\n", count));
            }
            
            // Question about students
            if (msg.contains("học viên") || msg.contains("sinh viên")) {
                log.info("✅ Detected student count question");
                Long totalStudents = systemStatsService.getTotalStudents();
                Long activeStudents = systemStatsService.getActiveStudents();
                if (!hasRealtimeData) {
                    dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                    dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n", today.toString()));
                    dataContext.append("⚠️ BẮT BUỘC dùng ngày này làm 'hôm nay', KHÔNG ĐƯỢC dùng ngày khác!\n\n");
                    hasRealtimeData = true;
                }
                dataContext.append(String.format("- Tổng số học viên HIỆN TẠI: %d người\n", totalStudents));
                dataContext.append(String.format("- Học viên đang hoạt động: %d người\n", activeStudents));
                dataContext.append(String.format("- Học viên chưa đăng ký lớp: %d người\n", totalStudents - activeStudents));
            }
            
            // Question about class start date
            if (msg.contains("lớp") && (msg.contains("bao giờ") || msg.contains("khi nào") || 
                msg.contains("còn") || msg.contains("bắt đầu") || msg.contains("khai giảng"))) {
                
                // Remove quotes and "của tôi" for better matching
                String cleanedMessage = userMessage.replace("\"", "").replace("'", "")
                    .replaceAll("của\\s+tôi", "").trim();
                
                // Pattern to extract class name (more flexible)
                Pattern classPattern = Pattern.compile("lớp\\s+([\\w\\s\\-ơưăâêôúíóáéýừứửữựăắằẳẵặâầấẩẫậêềếểễệôồốổỗộơờớởỡợưừứửữựđ]+?)(?:\\s+(?:còn|bao|khi|nữa|tính)|$)", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
                Matcher matcher = classPattern.matcher(cleanedMessage);
                
                if (matcher.find()) {
                    String className = matcher.group(1).trim();
                    log.info("✅ Detected class question for: '{}'", className);
                    
                    Map<String, Object> data = systemStatsService.getClassStartInfo(className);
                    log.info("🔍 Database lookup result: {}", data);
                    
                    if (data.get("success") != null && (Boolean) data.get("success")) {
                        if (!hasRealtimeData) {
                            dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                            dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n", today.toString()));
                            dataContext.append("⚠️ BẮT BUỘC dùng ngày này làm 'hôm nay', KHÔNG ĐƯỢC dùng ngày khác!\n\n");
                            hasRealtimeData = true;
                        }
                        dataContext.append("⚠️ QUAN TRỌNG: Dữ liệu này đã được tính toán chính xác từ database. KHÔNG ĐƯỢC tự tính lại!\n\n");
                        dataContext.append(String.format("THÔNG TIN LỚP HỌC:\n"));
                        dataContext.append(String.format("- Tên lớp: %s\n", data.get("class_name")));
                        dataContext.append(String.format("- Ngày khai giảng: %s\n", data.get("start_date")));
                        dataContext.append(String.format("- SỐ NGÀY CÒN LẠI: %s ngày (đã tính từ hôm nay)\n", data.get("days_until_start")));
                        dataContext.append(String.format("- Trạng thái: %s\n", data.get("status")));
                        dataContext.append(String.format("- Chi tiết: %s\n\n", data.get("message")));
                        dataContext.append("⚠️ CHÚ Ý: Trả lời số ngày chính xác là '" + data.get("days_until_start") + " ngày', KHÔNG ĐƯỢC tính toán lại!\n");
                        log.info("✅ Added real-time class data to context");
                    } else {
                        if (!hasRealtimeData) {
                            dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                            hasRealtimeData = true;
                        }
                        dataContext.append(String.format("⚠️ %s\n", data.get("error")));
                        log.warn("❌ Class not found in database: '{}'", className);
                    }
                } else {
                    log.info("❌ Could not extract class name from message");
                }
            }
            
            // Question about system overview
            if (msg.contains("thống kê") || msg.contains("tổng quan")) {
                log.info("✅ Detected system stats question");
                Map<String, Object> data = systemStatsService.getSystemStats();
                if (!hasRealtimeData) {
                    dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                    dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n", today.toString()));
                    dataContext.append("⚠️ BẮT BUỘC dùng ngày này làm 'hôm nay', KHÔNG ĐƯỢC dùng ngày khác!\n\n");
                    hasRealtimeData = true;
                }
                dataContext.append("THỐNG KÊ TỔNG QUAN HỆ THỐNG HIỆN TẠI:\n");
                dataContext.append(String.format("- Tổng số người dùng: %s người\n", data.get("total_users")));
                dataContext.append(String.format("- Tổng số học viên: %s người\n", data.get("total_students")));
                dataContext.append(String.format("- Học viên đang hoạt động: %s người\n", data.get("active_students")));
                dataContext.append(String.format("- Tổng số giảng viên: %s người\n", data.get("total_instructors")));
                dataContext.append(String.format("- Tổng số quản trị viên: %s người\n", data.get("total_admins")));
                dataContext.append(String.format("- Ngày cập nhật: %s (HÔM NAY)\n", data.get("timestamp")));
            }
            
        } catch (Exception e) {
            log.error("Failed to fetch realtime data", e);
            if (!hasRealtimeData) {
                dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
            }
            dataContext.append("⚠️ Không thể lấy dữ liệu thời gian thực từ hệ thống.\n");
        }
        
        // Add document context at the end if exists
        if (baseContext != null && !baseContext.isEmpty()) {
            dataContext.append("\n\n=== TÀI LIỆU THAM KHẢO (Chỉ dùng nếu không có dữ liệu realtime) ===\n");
            dataContext.append(baseContext);
        }
        
        String result = dataContext.toString();
        log.info("📤 Final context length: {} chars, has realtime data: {}", result.length(), hasRealtimeData);
        return result;
    }
    
    /**
     * Call internal API endpoint
     */
    private Map<String, Object> callInternalAPI(String endpoint, String jwtToken) {
        try {
            WebClient webClient = webClientBuilder
                .baseUrl("http://localhost:" + serverPort)
                .build();
            
            String response = webClient.get()
                .uri(endpoint)
                .header("Authorization", "Bearer " + jwtToken)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            
            return objectMapper.readValue(response, Map.class);
            
        } catch (Exception e) {
            log.error("Failed to call internal API: {}", endpoint, e);
            return new HashMap<>();
        }
    }
}
