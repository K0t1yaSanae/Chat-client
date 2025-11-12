package net.kotiyasanae.chatclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.kotiyasanae.chatclient.model.Message;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChatClient extends JFrame {
    private WebSocketClient webSocketClient;
    private ChatSocket chatSocket;
    private String username;
    private ObjectMapper mapper = new ObjectMapper();

    // UI组件
    private JTextArea messagesArea;
    private JTextField messageField;
    private JTextField usernameField;
    private JTextField serverHostField;
    private JTextField serverPortField;
    private JButton sendButton;
    private JButton connectButton;
    private JButton disconnectButton;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JLabel statusLabel;

    public ChatClient() {
        initializeUI();
        setupEventListeners();
    }

    private void initializeUI() {
        setTitle("Java聊天室客户端");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 顶部连接面板
        mainPanel.add(createConnectionPanel(), BorderLayout.NORTH);

        // 中央聊天区域
        mainPanel.add(createChatPanel(), BorderLayout.CENTER);

        // 右侧用户列表
        mainPanel.add(createUserPanel(), BorderLayout.EAST);

        setContentPane(mainPanel);
    }

    private JPanel createConnectionPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        panel.setBorder(new TitledBorder("连接设置"));

        // 第一行：服务器设置
        JPanel serverPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        serverPanel.add(new JLabel("服务器:"));

        serverHostField = new JTextField("localhost", 10);
        serverHostField.setToolTipText("输入服务器IP地址或主机名");
        serverPanel.add(serverHostField);

        serverPanel.add(new JLabel("端口:"));
        serverPortField = new JTextField("9000", 5);
        serverPortField.setToolTipText("输入服务器端口号");
        serverPanel.add(serverPortField);

        // 第二行：用户设置和按钮
        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        userPanel.add(new JLabel("用户名:"));

        usernameField = new JTextField(10);
        usernameField.setText("");
        userPanel.add(usernameField);

        connectButton = new JButton("连接");
        connectButton.setBackground(new Color(220, 220, 220)); // 浅灰色背景
        connectButton.setForeground(Color.BLACK); // 黑色文字
        userPanel.add(connectButton);

        disconnectButton = new JButton("断开");
        disconnectButton.setBackground(new Color(220, 220, 220)); // 浅灰色背景
        disconnectButton.setForeground(Color.BLACK); // 黑色文字
        disconnectButton.setEnabled(false);
        userPanel.add(disconnectButton);

        statusLabel = new JLabel("未连接");
        statusLabel.setForeground(Color.RED);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        userPanel.add(statusLabel);

        panel.add(serverPanel);
        panel.add(userPanel);

        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("聊天内容"));

        // 消息显示区域
        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setBackground(Color.WHITE);
        messagesArea.setFont(new Font("宋体", Font.PLAIN, 12));
        messagesArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JScrollPane scrollPane = new JScrollPane(messagesArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        // 消息输入区域
        JPanel inputPanel = new JPanel(new BorderLayout(5, 5));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        messageField = new JTextField();
        messageField.setEnabled(false);
        messageField.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        sendButton = new JButton("发送");
        sendButton.setEnabled(false);
        sendButton.setBackground(new Color(220, 220, 220)); // 浅灰色背景
        sendButton.setForeground(Color.BLACK); // 黑色文字
        sendButton.setPreferredSize(new Dimension(80, 25));

        inputPanel.add(new JLabel("消息:"), BorderLayout.WEST);
        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createUserPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(new TitledBorder("在线用户"));
        panel.setPreferredSize(new Dimension(180, 0));

        userListModel = new DefaultListModel<>();
        userListModel.addElement("暂无在线用户");

        userList = new JList<>(userListModel);
        userList.setBackground(Color.WHITE);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        userList.setFont(new Font("宋体", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(userList);

        // 操作按钮
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        JButton clearButton = new JButton("清屏");
        JButton refreshButton = new JButton("刷新");

        clearButton.setBackground(new Color(220, 220, 220)); // 浅灰色背景
        clearButton.setForeground(Color.BLACK); // 黑色文字
        refreshButton.setBackground(new Color(220, 220, 220)); // 浅灰色背景
        refreshButton.setForeground(Color.BLACK); // 黑色文字

        clearButton.addActionListener(e -> clearChat());
        refreshButton.addActionListener(e -> refreshUserList());

        buttonPanel.add(clearButton);
        buttonPanel.add(refreshButton);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void setupEventListeners() {
        // 连接按钮事件
        connectButton.addActionListener(e -> connectToServer());

        // 断开按钮事件
        disconnectButton.addActionListener(e -> disconnect());

        // 发送按钮事件
        sendButton.addActionListener(e -> sendMessage());

        // 回车发送消息
        messageField.addActionListener(e -> sendMessage());

        // 窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnect();
            }
        });
    }

    private void connectToServer() {
        if (chatSocket != null && chatSocket.isConnected()) {
            appendMessage("系统", "已经连接到服务器");
            return;
        }

        username = usernameField.getText().trim();
        String host = serverHostField.getText().trim();
        String portStr = serverPortField.getText().trim();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入用户名", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (username.length() > 20) {
            JOptionPane.showMessageDialog(this, "用户名不能超过20个字符", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (host.isEmpty()) {
            JOptionPane.showMessageDialog(this, "请输入服务器地址", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "端口号必须是1-65535之间的数字", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            webSocketClient = new WebSocketClient();
            webSocketClient.start();

            String serverUri = "ws://" + host + ":" + port + "/chat";
            chatSocket = new ChatSocket(this);

            ClientUpgradeRequest request = new ClientUpgradeRequest();
            webSocketClient.connect(chatSocket, new URI(serverUri), request);

            updateStatus("连接中...", Color.ORANGE);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            serverHostField.setEnabled(false);
            serverPortField.setEnabled(false);
            usernameField.setEnabled(false);

            // 等待连接建立
            new Thread(() -> {
                try {
                    boolean connected = chatSocket.awaitConnection(5, TimeUnit.SECONDS);
                    if (!connected) {
                        SwingUtilities.invokeLater(() -> {
                            updateStatus("连接超时", Color.RED);
                            resetConnectionState();
                            JOptionPane.showMessageDialog(ChatClient.this, "连接服务器超时", "连接错误", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "连接失败: " + e.getMessage(), "连接错误", JOptionPane.ERROR_MESSAGE);
            updateStatus("连接失败", Color.RED);
            resetConnectionState();
        }
    }

    private void disconnect() {
        if (webSocketClient != null) {
            try {
                webSocketClient.stop();
            } catch (Exception e) {
                System.err.println("断开连接错误: " + e.getMessage());
            }
        }

        if (chatSocket != null && chatSocket.isConnected()) {
            appendMessage("系统", "已断开服务器连接");
        }

        resetConnectionState();
        updateStatus("未连接", Color.RED);
    }

    private void resetConnectionState() {
        SwingUtilities.invokeLater(() -> {
            connectButton.setEnabled(true);
            disconnectButton.setEnabled(false);
            sendButton.setEnabled(false);
            messageField.setEnabled(false);
            serverHostField.setEnabled(true);
            serverPortField.setEnabled(true);
            usernameField.setEnabled(true);

            // 清空用户列表
            userListModel.clear();
            userListModel.addElement("暂无在线用户");
        });
    }

    private void sendMessage() {
        if (chatSocket == null || !chatSocket.isConnected()) {
            JOptionPane.showMessageDialog(this, "未连接到服务器", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String content = messageField.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        try {
            Message message = new Message();
            message.setType(Message.MessageType.CHAT);
            message.setSender(username);
            message.setContent(content);
            message.setTimestamp(java.time.LocalDateTime.now().toString());

            chatSocket.sendMessage(mapper.writeValueAsString(message));
            messageField.setText("");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "发送消息失败: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void appendMessage(String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            String formattedMessage = String.format("[%s] %s: %s\n", timestamp, sender, content);
            messagesArea.append(formattedMessage);

            // 自动滚动到底部
            messagesArea.setCaretPosition(messagesArea.getDocument().getLength());
        });
    }

    public void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
        });
    }

    private void clearChat() {
        messagesArea.setText("");
        appendMessage("系统", "聊天记录已清空");
    }

    private void refreshUserList() {
        appendMessage("系统", "刷新用户列表");
    }

    public void updateUserList(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            if (users != null && !users.isEmpty()) {
                for (String user : users) {
                    userListModel.addElement(user);
                }
            } else {
                userListModel.addElement("暂无在线用户");
            }
        });
    }

    public void onConnected() {
        SwingUtilities.invokeLater(() -> {
            updateStatus("已连接", new Color(0, 100, 0)); // 深绿色
            sendButton.setEnabled(true);
            messageField.setEnabled(true);
            messageField.requestFocus();
            appendMessage("系统", "成功连接到服务器");

            // 发送加入消息
            try {
                Message joinMessage = new Message();
                joinMessage.setType(Message.MessageType.JOIN);
                joinMessage.setSender(username);
                joinMessage.setContent("");
                joinMessage.setTimestamp(java.time.LocalDateTime.now().toString());

                chatSocket.sendMessage(mapper.writeValueAsString(joinMessage));
            } catch (Exception e) {
                appendMessage("系统", "发送加入消息失败: " + e.getMessage());
            }
        });
    }

    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            updateStatus("连接关闭", Color.RED);
            resetConnectionState();
            appendMessage("系统", "连接已断开: " + reason);
        });
    }

    public void onError(String error) {
        SwingUtilities.invokeLater(() -> {
            updateStatus("连接错误", Color.RED);
            resetConnectionState();
            appendMessage("系统", "连接错误: " + error);
        });
    }

    public void handleWebSocketMessage(String jsonMessage) {
        try {
            Message message = mapper.readValue(jsonMessage, Message.class);

            switch (message.getType()) {
                case CHAT:
                    appendMessage(message.getSender(), message.getContent());
                    break;
                case SYSTEM:
                    handleSystemMessage(message);
                    break;
                case JOIN:
                    appendMessage("系统", message.getContent());
                    updateUserListFromJoinLeave(message.getContent(), true);
                    break;
                case LEAVE:
                    appendMessage("系统", message.getContent());
                    updateUserListFromJoinLeave(message.getContent(), false);
                    break;
                case ERROR:
                    appendMessage("错误", message.getContent());
                    break;
            }

        } catch (Exception e) {
            System.err.println("解析消息失败: " + e.getMessage());
            appendMessage("系统", "收到无法解析的消息: " + jsonMessage);
        }
    }

    // 修改 handleSystemMessage 方法，添加清屏处理
    private void handleSystemMessage(Message message) {
        String content = message.getContent();

        // 检查是否是清屏命令
        if ("CLEAR_CHAT".equals(content)) {
            clearChatWithoutSystemMessage();
            return;
        }

        appendMessage("系统", content);

        // 检查是否是用户列表消息
        if (content.contains("当前在线用户")) {
            extractUserListFromSystemMessage(content);
        }
    }

    // 修改清屏方法，去掉系统消息
    private void clearChatWithoutSystemMessage() {
        SwingUtilities.invokeLater(() -> {
            messagesArea.setText("");
            // 移除了 appendMessage("系统", "聊天记录已清空");
        });
    }

    // 从系统消息中提取用户列表
    private void extractUserListFromSystemMessage(String systemMessage) {
        try {
            // 格式: "当前在线用户 (2): user1, user2"
            Pattern pattern = Pattern.compile("当前在线用户 \\((\\d+)\\): (.+)");
            Matcher matcher = pattern.matcher(systemMessage);

            if (matcher.find()) {
                String userListStr = matcher.group(2);
                String[] users = userListStr.split(", ");

                List<String> userList = new ArrayList<>();
                for (String user : users) {
                    if (!user.trim().isEmpty()) {
                        userList.add(user.trim());
                    }
                }

                updateUserList(userList);
            }
        } catch (Exception e) {
            System.err.println("提取用户列表失败: " + e.getMessage());
        }
    }

    // 从加入/离开消息更新用户列表
    private void updateUserListFromJoinLeave(String message, boolean isJoin) {
        try {
            // 格式: "用户名 加入了聊天室" 或 "用户名 离开了聊天室"
            String username = message.replace(" 加入了聊天室", "").replace(" 离开了聊天室", "");

            SwingUtilities.invokeLater(() -> {
                if (isJoin) {
                    // 用户加入
                    if (!userListModel.contains(username)) {
                        userListModel.addElement(username);
                    }
                } else {
                    // 用户离开
                    userListModel.removeElement(username);
                }

                // 如果没有用户在线，显示提示
                if (userListModel.size() == 0) {
                    userListModel.addElement("暂无在线用户");
                }
            });
        } catch (Exception e) {
            System.err.println("更新用户列表失败: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // 使用系统默认外观，移除圆角设计
        try {
            // 设置系统默认外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            new ChatClient().setVisible(true);
        });
    }
}