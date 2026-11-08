package ru.migrenenet.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Пациент, подписанный на утреннее напоминание в Telegram. */
@Entity
public class Subscriber {
    @Id private Long chatId;
    private boolean active = true;

    protected Subscriber() {}
    public Subscriber(Long chatId) { this.chatId = chatId; }
    public Long getChatId() { return chatId; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { active = v; }
}
