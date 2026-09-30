package devicemaintenance.controller;

import devicemaintenance.service.SimpleNotificationListenerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Device notification query controller
 * Provides auxiliary APIs to view received Kafka notifications
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class DeviceNotificationController {

    private final SimpleNotificationListenerService notificationListener;

    /**
     * Get all stored notifications
     */
    @GetMapping("/all")
    public Map<String, Object> getAllNotifications() {
        log.info("Query all notifications");
        List<SimpleNotificationListenerService.NotificationRecord> notifications = 
            notificationListener.getAllNotifications();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("total", notifications.size());
        response.put("notifications", notifications);
        
        return response;
    }

    /**
     * Get recent N notifications
     */
    @GetMapping("/recent")
    public Map<String, Object> getRecentNotifications(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("Query recent {} notifications", limit);
        List<SimpleNotificationListenerService.NotificationRecord> notifications = 
            notificationListener.getRecentNotifications(limit);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("limit", limit);
        response.put("total", notifications.size());
        response.put("notifications", notifications);
        
        return response;
    }

    /**
     * Get notifications by topic
     */
    @GetMapping("/by-topic/{topic}")
    public Map<String, Object> getNotificationsByTopic(
            @PathVariable String topic) {
        log.info("Query notifications for topic: {}", topic);
        List<SimpleNotificationListenerService.NotificationRecord> notifications = 
            notificationListener.getNotificationsByTopic(topic);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("topic", topic);
        response.put("total", notifications.size());
        response.put("notifications", notifications);
        
        return response;
    }

    /**
     * Search notifications by keyword
     */
    @GetMapping("/search")
    public Map<String, Object> searchNotifications(
            @RequestParam String keyword) {
        log.info("Search notifications with keyword: {}", keyword);
        List<SimpleNotificationListenerService.NotificationRecord> notifications = 
            notificationListener.searchNotifications(keyword);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("keyword", keyword);
        response.put("total", notifications.size());
        response.put("notifications", notifications);
        
        return response;
    }

    /**
     * Get notification statistics
     */
    @GetMapping("/stats")
    public Map<String, Object> getNotificationStats() {
        log.info("Query notification statistics");
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("totalNotifications", notificationListener.getNotificationCount());
        response.put("maxCapacity", 200);
        
        return response;
    }

    /**
     * Clear all notifications
     */
    @DeleteMapping("/clear")
    public Map<String, Object> clearNotifications() {
        log.info("Clear all notifications");
        int count = notificationListener.getNotificationCount();
        notificationListener.clearNotifications();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Cleared " + count + " notifications");
        
        return response;
    }
}

