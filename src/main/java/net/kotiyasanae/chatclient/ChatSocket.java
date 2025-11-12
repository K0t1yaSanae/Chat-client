package net.kotiyasanae.chatclient;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@WebSocket
public class ChatSocket {
    private Session session;
    private ChatClient chatClient;
    private CountDownLatch connectionLatch = new CountDownLatch(1);

    public ChatSocket(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        System.out.println("WebSocket连接已建立");
        connectionLatch.countDown();
        chatClient.onConnected();
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        System.out.println("WebSocket连接关闭: " + reason);
        chatClient.onDisconnected(reason);
    }

    @OnWebSocketError
    public void onError(Throwable error) {
        System.err.println("WebSocket错误: " + error.getMessage());
        chatClient.onError(error.getMessage());
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        System.out.println("收到消息: " + message);
        chatClient.handleWebSocketMessage(message);
    }

    public void sendMessage(String message) throws IOException {
        if (session != null && session.isOpen()) {
            session.getRemote().sendString(message);
        } else {
            throw new IOException("WebSocket连接未就绪");
        }
    }

    public boolean isConnected() {
        return session != null && session.isOpen();
    }

    public boolean awaitConnection(long timeout, TimeUnit unit) throws InterruptedException {
        return connectionLatch.await(timeout, unit);
    }
}