package com.example.lifecoach3.controller;

import com.example.lifecoach3.model.Conversation;
import com.example.lifecoach3.model.Message;
import com.example.lifecoach3.model.User;
import com.example.lifecoach3.repository.UserRepository;
import com.example.lifecoach3.service.ChatService;
import com.example.lifecoach3.service.ConversationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final ConversationService conversationService;
    private final UserRepository userRepository;

    @Autowired
    public ChatController(ChatService chatService, ConversationService conversationService,
                          UserRepository userRepository) {
        this.chatService = chatService;
        this.conversationService = conversationService;
        this.userRepository = userRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody Map<String, String> request, HttpSession session) {
        String message = request.get("message");
        Long conversationId = null;

        try {
            conversationId = Long.parseLong(request.get("conversationId"));
        } catch (NumberFormatException | NullPointerException e) {
            // 如果没有提供有效的conversationId，将继续处理但不保存消息
        }

        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> response = new HashMap<>();

        if (message == null || message.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "消息内容不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        // 处理用户消息并生成回复
        String botResponse = chatService.processUserMessage(message);

        // 如果用户已登录且提供了有效的conversationId，保存消息
        if (userId != null && conversationId != null) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isPresent()) {
                Optional<Conversation> conversationOpt = conversationService.getConversationById(conversationId, userOpt.get());
                if (conversationOpt.isPresent()) {
                    Message savedMessage = chatService.saveMessage(conversationOpt.get(), message, botResponse);

                    response.put("messageId", savedMessage.getId());
                }
            }
        }

        response.put("success", true);
        response.put("botResponse", botResponse);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations")
    public ResponseEntity<?> getConversations(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.status(404).body(response);
        }

        List<Conversation> conversations = conversationService.getUserConversations(userOpt.get());
        List<Map<String, Object>> conversationDTOs = conversations.stream()
                .map(conv -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("conversation_id", conv.getId());
                    dto.put("title", conv.getTitle());
                    dto.put("updated_at", conv.getUpdatedAt());

                    // 获取第一条消息作为预览（如果有）
                    if (!conv.getMessages().isEmpty() && conv.getMessages().get(0) != null) {
                        dto.put("first_message", conv.getMessages().get(0).getUserMessage());
                    } else {
                        dto.put("first_message", "");
                    }

                    return dto;
                })
                .collect(Collectors.toList());

        response.put("success", true);
        response.put("conversations", conversationDTOs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<?> getConversationMessages(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.status(404).body(response);
        }

        Optional<Conversation> conversationOpt = conversationService.getConversationWithMessages(id, userOpt.get());
        if (!conversationOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "对话不存在或无权访问");
            return ResponseEntity.status(404).body(response);
        }

        List<Message> messages = chatService.getConversationMessages(conversationOpt.get());
        List<Map<String, Object>> messageDTOs = messages.stream()
                .map(msg -> {
                    Map<String, Object> dto = new HashMap<>();
                    dto.put("message_id", msg.getId());
                    dto.put("user_message", msg.getUserMessage());
                    dto.put("bot_response", msg.getBotResponse());
                    dto.put("created_at", msg.getCreatedAt());
                    return dto;
                })
                .collect(Collectors.toList());

        response.put("success", true);
        response.put("messages", messageDTOs);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversations")
    public ResponseEntity<?> createConversation(@RequestBody Map<String, String> request, HttpSession session) {
        String title = request.get("title");
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.status(404).body(response);
        }

        if (title == null || title.trim().isEmpty()) {
            title = "新对话";
        }

        Conversation conversation = conversationService.createConversation(userOpt.get(), title);

        response.put("success", true);
        response.put("conversation_id", conversation.getId());
        response.put("title", conversation.getTitle());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<?> deleteConversation(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.status(404).body(response);
        }

        boolean deleted = conversationService.deleteConversation(id, userOpt.get());

        if (deleted) {
            response.put("success", true);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "对话不存在或无权删除");
            return ResponseEntity.status(404).body(response);
        }
    }

    @PutMapping("/conversations/{id}")
    public ResponseEntity<?> updateConversation(@PathVariable Long id,
                                                @RequestBody Map<String, String> request,
                                                HttpSession session) {
        String title = request.get("title");
        Long userId = (Long) session.getAttribute("userId");
        Map<String, Object> response = new HashMap<>();

        if (userId == null) {
            response.put("success", false);
            response.put("message", "用户未登录");
            return ResponseEntity.status(401).body(response);
        }

        if (title == null || title.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "标题不能为空");
            return ResponseEntity.badRequest().body(response);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.status(404).body(response);
        }

        Optional<Conversation> updatedConversation = conversationService.updateConversationTitle(id, userOpt.get(), title);

        if (updatedConversation.isPresent()) {
            response.put("success", true);
            response.put("conversation_id", updatedConversation.get().getId());
            response.put("title", updatedConversation.get().getTitle());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "对话不存在或无权更新");
            return ResponseEntity.status(404).body(response);
        }
    }
}