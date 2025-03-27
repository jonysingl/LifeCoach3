package com.example.lifecoach3.controller;

import com.example.lifecoach3.model.Conversation;
import com.example.lifecoach3.model.Message;
import com.example.lifecoach3.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private ChatService chatService;

    // 检查登录状态
    @GetMapping("/check-login")
    public Map<String, Object> checkLogin(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal != null) {
            response.put("loggedIn", true);
            response.put("username", principal.getName());
        } else {
            response.put("loggedIn", false);
        }
        return response;
    }

    // 获取对话列表
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations(Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        try {
            List<Conversation> conversations = chatService.getConversationsByUsername(principal.getName());
            response.put("success", true);
            response.put("conversations", conversations);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取对话列表失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 创建新对话
    @PostMapping("/conversations")
    public ResponseEntity<Map<String, Object>> createConversation(
            @RequestBody Map<String, String> payload,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        try {
            String title = payload.get("title");
            Conversation conversation = chatService.createConversation(principal.getName(), title);
            response.put("success", true);
            response.put("conversation_id", conversation.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "创建对话失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 获取对话消息
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable Long conversationId,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        try {
            // 验证对话属于当前用户
            boolean hasAccess = chatService.checkConversationAccess(conversationId, principal.getName());
            if (!hasAccess) {
                response.put("success", false);
                response.put("message", "无权访问此对话");
                return ResponseEntity.status(403).body(response);
            }

            List<Message> messages = chatService.getMessagesByConversationId(conversationId);
            response.put("success", true);
            response.put("messages", messages);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "获取消息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 更新对话标题
    @PutMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, Object>> updateConversation(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> payload,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean hasAccess = chatService.checkConversationAccess(conversationId, principal.getName());
            if (!hasAccess) {
                response.put("success", false);
                response.put("message", "无权修改此对话");
                return ResponseEntity.status(403).body(response);
            }

            String title = payload.get("title");
            chatService.updateConversationTitle(conversationId, title);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "更新对话失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 删除对话
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, Object>> deleteConversation(
            @PathVariable Long conversationId,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();
        if (principal == null) {
            response.put("success", false);
            response.put("message", "未登录");
            return ResponseEntity.status(401).body(response);
        }

        try {
            boolean hasAccess = chatService.checkConversationAccess(conversationId, principal.getName());
            if (!hasAccess) {
                response.put("success", false);
                response.put("message", "无权删除此对话");
                return ResponseEntity.status(403).body(response);
            }

            chatService.deleteConversation(conversationId);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "删除对话失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // 处理聊天消息
    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> getResponse(
            @RequestParam("msg") String message,
            @RequestParam(value = "conversation_id", required = false) Long conversationId,
            Principal principal) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 如果用户已登录且提供了对话ID
            if (principal != null && conversationId != null) {
                boolean hasAccess = chatService.checkConversationAccess(conversationId, principal.getName());
                if (!hasAccess) {
                    response.put("success", false);
                    response.put("message", "无权访问此对话");
                    return ResponseEntity.status(403).body(response);
                }
            }

            String botResponse = chatService.processMessage(message, conversationId, principal != null ? principal.getName() : null);
            response.put("response", botResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "处理消息失败: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}