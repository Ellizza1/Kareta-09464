package ru.gr0946x.ui;

import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatWindow extends JFrame implements Ui {
    
    // Список слушателей для отправки данных ИЗ окна В класс Client
    private final List<Consumer<String>> listeners = new ArrayList<>();
    
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> listModel;

    public ChatWindow() {
        setTitle("Мессенджер Карета");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Поле чата
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Список пользователей
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setPreferredSize(new Dimension(150, 0));
        listModel.addElement("Общий чат (всем)");
        userList.setSelectedIndex(0); 
        add(new JScrollPane(userList), BorderLayout.EAST);

        // Панель ввода
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton sendButton = new JButton("Отправить");

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            // Отправляем чистый текст в Client
            for (Consumer<String> listener : listeners) {
                listener.accept(text);
            }
            messageField.setText("");
        }
    }

    @Override
    public void start() {
        SwingUtilities.invokeLater(() -> this.setVisible(true));

    }
    // @Override
    // public void start() {
    //     SwingUtilities.invokeLater(() -> {
    //         this.setVisible(true);
    //         // Принудительно выводим стартовую строку сразу при открытии окна
    //         chatArea.append("[Система]: Подключение к серверу успешно...\n");
    //         chatArea.append("[Система]: Введите имя в поле ниже для входа:\n");
    //     });
    // }

    // Этот метод вызывается, когда Client получает данные от сервера и передает их в UI
    @Override
    public void showInfo(final String data, final MessageType type) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                switch (type) {
                    case MESSAGE:
                        String[] message = data.split(ProtocolConstants.AUTHOR_SEPARATOR, 2);
                        if (message.length == 2) {
                            chatArea.append(message[0] + " написал: " + message[1] + "\n");
                        } else {
                            chatArea.append(data + "\n");
                        }
                        break;
                    case ERROR:
                        JOptionPane.showMessageDialog(ChatWindow.this, data, "Ошибка", JOptionPane.ERROR_MESSAGE);
                        break;
                    default: // Для типов INFO, REQUEST и т.д.
                        chatArea.append("[Система]: " + data + "\n");
                        break;
                }
            }
        });
    }

    @Override
    public void addUserDataListener(Consumer<String> listener) {
        listeners.add(listener);
    }

    @Override
    public void removeUserDataListener(Consumer<String> listener) {
        listeners.remove(listener);
    }
}