package com.example.lifecoach3.service;

import com.example.lifecoach3.model.Conversation;
import com.example.lifecoach3.model.Message;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ChatService {

    // 简单的内存存储，实际应用中应该使用数据库
    private final Map<Long, Conversation> conversations = new HashMap<>();
    private final Map<Long, List<Message>> messages = new HashMap<>();
    private final Map<String, List<Long>> userConversations = new HashMap<>();
    private final AtomicLong conversationCounter = new AtomicLong(1);
    private final AtomicLong messageCounter = new AtomicLong(1);

    // 获取用户的所有对话
    public List<Conversation> getConversationsByUsername(String username) {
        List<Conversation> result = new ArrayList<>();
        if (userConversations.containsKey(username)) {
            for (Long id : userConversations.get(username)) {
                result.add(conversations.get(id));
            }
        }
        return result;
    }

    // 创建新对话
    public Conversation createConversation(String username, String title) {
        Long id = conversationCounter.getAndIncrement();
        Conversation conversation = new Conversation();
        conversation.setId(id);
        conversation.setUsername(username);
        conversation.setTitle(title);
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());

        conversations.put(id, conversation);
        userConversations.computeIfAbsent(username, k -> new ArrayList<>()).add(id);
        messages.put(id, new ArrayList<>());

        return conversation;
    }

    // 检查用户是否有权访问对话
    public boolean checkConversationAccess(Long conversationId, String username) {
        Conversation conversation = conversations.get(conversationId);
        return conversation != null && conversation.getUsername().equals(username);
    }

    // 获取对话中的消息
    public List<Message> getMessagesByConversationId(Long conversationId) {
        return messages.getOrDefault(conversationId, new ArrayList<>());
    }

    // 更新对话标题
    public void updateConversationTitle(Long conversationId, String title) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation != null) {
            conversation.setTitle(title);
            conversation.setUpdatedAt(LocalDateTime.now());
        }
    }

    // 删除对话
    public void deleteConversation(Long conversationId) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation != null) {
            String username = conversation.getUsername();
            userConversations.get(username).remove(conversationId);
            conversations.remove(conversationId);
            messages.remove(conversationId);
        }
    }

    // 处理用户消息并返回机器人回复
    public String processMessage(String userMessage, Long conversationId, String username) {
        // 简单的回复逻辑，实际应用中可以接入对话模型API
        String botResponse = generateResponse(userMessage);

        // 如果用户已登录且提供了对话ID，则保存消息
        if (username != null) {
            if (conversationId != null && conversations.containsKey(conversationId)) {
                saveMessage(conversationId, userMessage, botResponse);
            }
        }

        return botResponse;
    }

    // 保存消息
    private void saveMessage(Long conversationId, String userMessage, String botResponse) {
        Message message = new Message();
        message.setId(messageCounter.getAndIncrement());
        message.setConversationId(conversationId);
        message.setUserMessage(userMessage);
        message.setBotResponse(botResponse);
        message.setCreatedAt(LocalDateTime.now());

        messages.get(conversationId).add(message);

        // 更新对话的最近更新时间
        Conversation conversation = conversations.get(conversationId);
        conversation.setUpdatedAt(LocalDateTime.now());

        // 如果这是对话的第一条消息，设置预览文本
        if (messages.get(conversationId).size() == 1) {
            conversation.setFirstMessage(userMessage);
        }
    }

    // 生成回复
    private String generateResponse(String userMessage) {
        // 简单的回复生成逻辑，实际应用中应对接更复杂的处理
        if (userMessage.toLowerCase().contains("你好") || userMessage.toLowerCase().contains("hello")) {
            return "您好！很高兴为您服务。有什么我可以帮助您的吗？";
        } else if (userMessage.toLowerCase().contains("谢谢") || userMessage.toLowerCase().contains("thank")) {
            return "不客气！如果您还有其他问题，随时可以问我。";
        } else if (userMessage.toLowerCase().contains("再见") || userMessage.toLowerCase().contains("goodbye")) {
            return "再见！祝您有美好的一天！";
        } else {
            return "我理解您说的是「" + userMessage + "」，但我目前的能力有限，无法提供准确的回复。未来我会不断学习和进步，期待能更好地为您服务。";
        }
    }
}