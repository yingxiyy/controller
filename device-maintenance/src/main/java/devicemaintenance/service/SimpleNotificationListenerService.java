package devicemaintenance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Simple notification listener service
 * Stores raw notification payloads for debugging
 */
@Slf4j
@Component
public class SimpleNotificationListenerService {

    /**
     * Maximum number of notifications to store
     */
    private static final int MAX_STORED_NOTIFICATIONS = 200;

    /**
     * Thread-safe queue to store notifications
     */
    private final ConcurrentLinkedQueue<NotificationRecord> notificationQueue = new ConcurrentLinkedQueue<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Store a notification
     */
    public void storeNotification(String topic, String key, String payload) {
        try {
            log.info("🔔 Received notification from topic: {}, key: {}", topic, key);
            log.debug("Payload: {}", payload);

            NotificationRecord record = new NotificationRecord();
            record.setReceivedTime(LocalDateTime.now());
            record.setTopic(topic);
            record.setKey(key);
            record.setPayload(payload);
            
            // Try to parse as JSON for better readability
            try {
                Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
                record.setPayloadMap(payloadMap);
            } catch (Exception e) {
                log.debug("Payload is not JSON, storing as string");
            }

            // Store notification
            notificationQueue.offer(record);

            // Remove oldest if exceeds max size
            while (notificationQueue.size() > MAX_STORED_NOTIFICATIONS) {
                NotificationRecord removed = notificationQueue.poll();
                log.debug("Removed oldest notification: {}", removed);
            }

            log.info("✅ Stored notification. Total: {}", notificationQueue.size());
        } catch (Exception e) {
            log.error("Failed to store notification", e);
        }
    }

    /**
     * Get all stored notifications
     */
    public List<NotificationRecord> getAllNotifications() {
        List<NotificationRecord> list = new ArrayList<>(notificationQueue);
        Collections.reverse(list); // Newest first
        return list;
    }

    /**
     * Get recent N notifications
     */
    public List<NotificationRecord> getRecentNotifications(int limit) {
        List<NotificationRecord> all = getAllNotifications();
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(0, limit);
    }

    /**
     * Get notifications by topic
     */
    public List<NotificationRecord> getNotificationsByTopic(String topic) {
        List<NotificationRecord> result = new ArrayList<>();
        for (NotificationRecord record : notificationQueue) {
            if (topic.equals(record.getTopic())) {
                result.add(record);
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Search notifications by keyword in payload
     */
    public List<NotificationRecord> searchNotifications(String keyword) {
        List<NotificationRecord> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (NotificationRecord record : notificationQueue) {
            if (record.getPayload() != null && 
                record.getPayload().toLowerCase().contains(lowerKeyword)) {
                result.add(record);
            }
        }
        Collections.reverse(result);
        return result;
    }

    /**
     * Clear all notifications
     */
    public void clearNotifications() {
        int size = notificationQueue.size();
        notificationQueue.clear();
        log.info("Cleared {} notifications", size);
    }

    /**
     * Get notification count
     */
    public int getNotificationCount() {
        return notificationQueue.size();
    }

    /**
     * Notification record data class
     */
    @Data
    public static class NotificationRecord {
        private LocalDateTime receivedTime;
        private String topic;
        private String key;
        private String payload;
        private Map<String, Object> payloadMap;
    }
}

