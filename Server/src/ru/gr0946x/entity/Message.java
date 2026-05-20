package ru.gr0946x.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt = LocalDateTime.now();
    @Enumerated(EnumType.STRING)

    @Column(nullable = false)
    private MessageStatus status = MessageStatus.SENT;

    public MessageStatus getStatus() { return status; }
    public void setStatus(MessageStatus status) { this.status = status; }

    public Message() {}

    // Конструктор, который ты используешь в ServerMain
    public Message(User sender, String text) {
        this.sender = sender;
        this.text = text;
    }

    public Message(User sender, User recipient, String text) {
        this.sender = sender;
        this.recipient = recipient;
        this.text = text;
    }

    // Обязательно добавь сеттеры, чтобы Hibernate мог корректно работать с полями
    public void setSender(User sender) { this.sender = sender; }

    public void setRecipient(User recipient) { this.recipient = recipient; }

    public void setText(String text) { this.text = text; }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public Long getId() { return id; }
    public User getSender() { return sender; }
    public User getRecipient() { return recipient; }
    public String getText() { return text; }
    public LocalDateTime getSentAt() { return sentAt; }
}