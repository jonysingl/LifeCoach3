package com.example.lifecoach3.service;

import com.example.lifecoach3.model.Conversation;
import com.example.lifecoach3.model.User;
import com.example.lifecoach3.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final ConversationRepository conversationRepository;

    @Autowired
    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
    }

    @Transactional
    public Conversation createConversation(User user, String title) {
        Conversation conversation = new Conversation();
        conversation.setUser(user);
        conversation.setTitle(title);
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getUserConversations(User user) {
        return conversationRepository.findByUserOrderByUpdatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> getConversationById(Long id, User user) {
        return conversationRepository.findByIdAndUser(id, user);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> getConversationWithMessages(Long id, User user) {
        return conversationRepository.findByIdAndUserWithMessages(id, user);
    }

    @Transactional
    public boolean deleteConversation(Long id, User user) {
        Optional<Conversation> conversation = getConversationById(id, user);
        if (conversation.isPresent()) {
            conversationRepository.delete(conversation.get());
            return true;
        }
        return false;
    }

    @Transactional
    public Optional<Conversation> updateConversationTitle(Long id, User user, String newTitle) {
        Optional<Conversation> conversationOpt = getConversationById(id, user);
        if (conversationOpt.isPresent()) {
            Conversation conversation = conversationOpt.get();
            conversation.setTitle(newTitle);
            return Optional.of(conversationRepository.save(conversation));
        }
        return Optional.empty();
    }
}