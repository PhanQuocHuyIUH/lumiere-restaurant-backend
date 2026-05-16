package iuh.fit.se.ai.application.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.se.ai.client.dto.ConversationTurn;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatbotHistoryCacheService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatbotHistoryCacheService.class);
    private static final String KEY_PREFIX = "chatbot:history:";
    private static final int MAX_TURNS = 2;
    private static final long TTL_MINUTES = 30;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatbotHistoryCacheService(RedisTemplate<String, Object> redisTemplate,
                                      ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ConversationTurn> load(String sessionId) {
        String key = KEY_PREFIX + sessionId;
        try {
            Object raw = redisTemplate.opsForValue().get(key);
            if (raw instanceof String json) {
                return objectMapper.readValue(json, new TypeReference<List<ConversationTurn>>() {});
            }
        } catch (Exception ex) {
            LOGGER.warn("Redis error loading chatbot history for session {}", sessionId, ex);
        }
        return List.of();
    }

    public void appendAndSave(String sessionId, String userMessage, String assistantReply,
                               List<ConversationTurn> existing) {
        String key = KEY_PREFIX + sessionId;
        try {
            List<ConversationTurn> updated = new ArrayList<>(existing);
            updated.add(ConversationTurn.user(userMessage));
            updated.add(ConversationTurn.assistant(assistantReply));

            // Keep only last MAX_TURNS completed turns (= MAX_TURNS * 2 messages)
            int maxMessages = MAX_TURNS * 2;
            if (updated.size() > maxMessages) {
                updated = updated.subList(updated.size() - maxMessages, updated.size());
            }

            String json = objectMapper.writeValueAsString(updated);
            // Sliding TTL: always reset on every write
            redisTemplate.opsForValue().set(key, json, TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ex) {
            LOGGER.warn("Redis error saving chatbot history for session {}", sessionId, ex);
        }
    }

    public void delete(String sessionId) {
        try {
            redisTemplate.delete(KEY_PREFIX + sessionId);
        } catch (Exception ex) {
            LOGGER.warn("Redis error deleting chatbot history for session {}", sessionId, ex);
        }
    }
}
