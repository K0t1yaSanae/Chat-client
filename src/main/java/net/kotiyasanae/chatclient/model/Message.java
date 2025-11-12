package net.kotiyasanae.chatclient.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Message {
    private MessageType type;
    private String content;
    private String sender;
    private String timestamp;
    private String room;

    public enum MessageType {
        CHAT, JOIN, LEAVE, SYSTEM, ERROR, USER_LIST
    }

    public Message() {
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    public Message(MessageType type, String content, String sender) {
        this();
        this.type = type;
        this.content = content;
        this.sender = sender;
    }

    // Getters and Setters
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", timestamp, sender, content);
    }
}