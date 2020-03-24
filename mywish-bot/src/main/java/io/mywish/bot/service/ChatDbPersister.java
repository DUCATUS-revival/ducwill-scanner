package io.mywish.bot.service;

import io.lastwill.eventscan.model.SubscribedChat;
import io.lastwill.eventscan.repositories.SubscribedChatRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.stream.Collectors;

@Slf4j
public class ChatDbPersister implements ChatPersister {
    @Autowired
    private SubscribedChatRepository chatRepository;

    @PostConstruct
    protected void init() {
        log.info("Loaded {} chats.", getCount());
    }

    @Override
    public synchronized boolean tryAdd(long chatId, String botName) {
        if (chatRepository.existsByChatIdAndBotName(chatId, botName)) {
            return false;
        }

        chatRepository.save(new SubscribedChat(chatId, botName));
        return true;
    }

    @Override
    public Iterable<Long> getChatsByBotName(String botName) {
        return chatRepository.findAllByBotName(botName)
                .stream()
                .map(SubscribedChat::getChatId)
                .collect(Collectors.toSet());
    }

    @Override
    public Iterable<Long> getChats() {
        return chatRepository.findAll()
                .stream()
                .map(SubscribedChat::getChatId)
                .collect(Collectors.toSet());
    }

    @Override
    public int getCount() {
        return (int) chatRepository.count();
    }

    @Override
    public void remove(long chatId) {
        chatRepository.deleteByChatId(chatId);
    }
}
