package com.example.lifecoach3.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "conversation_id")
    private Long id;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    @Column(name = "first_message", columnDefinition = "text")
    private String firstMessage;

    @Column(name = "last_message_preview", length = 100)
    private String lastMessagePreview;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt ASC")
    private List<Message> messages = new ArrayList<>();

    public Conversation() {
        // 默认构造函数
    }

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getFirstMessage() {
        return firstMessage;
    }

    public void setFirstMessage(String firstMessage) {
        this.firstMessage = firstMessage;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        if (lastMessagePreview != null && lastMessagePreview.length() > 100) {
            lastMessagePreview = lastMessagePreview.substring(0, 97) + "...";
        }
        this.lastMessagePreview = lastMessagePreview;
    }

    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages != null ? messages : new ArrayList<>();
    }

    // Helper method to add a message
    public void addMessage(Message message) {
        if (message == null) return;

        messages.add(message);
        message.setConversation(this);

        // 如果是第一条消息，同时更新firstMessage和lastMessagePreview
        if (messages.size() == 1) {
            this.firstMessage = message.getUserMessage();
        }

        // 更新最后消息预览和更新时间
        updateLastMessagePreview(message.getUserMessage());
        updatedAt = new Date();
    }

    // Helper method to remove a message
    public void removeMessage(Message message) {
        if (message == null) return;

        messages.remove(message);
        message.setConversation(null);

        // 如果移除后还有消息，更新预览为最后一条
        if (!messages.isEmpty()) {
            updateLastMessagePreview(messages.get(messages.size() - 1).getUserMessage());
        } else {
            lastMessagePreview = null;
            firstMessage = null;
        }
        updatedAt = new Date();
    }

    // 辅助方法：更新最后消息预览
    private void updateLastMessagePreview(String messageContent) {
        if (messageContent == null) {
            this.lastMessagePreview = "";
            return;
        }

        if (messageContent.length() > 100) {
            this.lastMessagePreview = messageContent.substring(0, 97) + "...";
        } else {
            this.lastMessagePreview = messageContent;
        }
    }

    // 添加获取消息数量的便捷方法
    public int getMessageCount() {
        return messages.size();
    }

    // 添加判断是否为空对话的便捷方法
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    // 添加获取最后一条消息的便捷方法
    public Message getLastMessage() {
        return messages.isEmpty() ? null : messages.get(messages.size() - 1);
    }
}