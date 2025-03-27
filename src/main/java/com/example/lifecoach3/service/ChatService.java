package com.example.lifecoach3.service;

import com.example.lifecoach3.model.Conversation;
import com.example.lifecoach3.model.Message;
import com.example.lifecoach3.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatService {

    private final MessageRepository messageRepository;

    @Autowired
    public ChatService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Transactional
    public Message saveMessage(Conversation conversation, String userMessage, String botResponse) {
        Message message = new Message();
        message.setConversation(conversation);
        message.setUserMessage(userMessage);
        message.setBotResponse(botResponse);

        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(Conversation conversation) {
        return messageRepository.findByConversationOrderByCreatedAtAsc(conversation);
    }

    @Transactional
    public String processUserMessage(String userMessage) {
        // 这里实现对用户消息的处理逻辑，可以调用AI服务等
        // 简单示例：返回一个基本回复

        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "我没听清您说什么，请再说一次。";
        }

        // 简单响应逻辑，实际应用中应替换为AI处理
        if (userMessage.contains("你好") || userMessage.contains("您好")) {
            return "您好！有什么可以帮助您的吗？";
        } else if (userMessage.contains("天气")) {
            return "很抱歉，我暂时无法提供实时天气信息。不过我可以帮您解答生活中的其他问题！";
        } else if (userMessage.contains("谢谢") || userMessage.contains("感谢")) {
            return "不客气！我很高兴能帮到您。如果还有其他问题，随时可以问我。";
        } else if (userMessage.contains("再见")) {
            return "再见！有需要随时来找我。";
        }

        // 默认回复
        return "我理解您的问题是关于\"" + userMessage + "\"。我正在学习中，希望能更好地为您服务。您能详细描述一下您的问题吗？";
    }
}