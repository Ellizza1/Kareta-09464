package ru.gr0946x.ui;

import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ChatWindow extends JFrame implements Ui {
    
    private final List<Consumer<String>> listeners = new ArrayList<>();
    
    private JTextArea chatArea;
    private JTextField messageField;
    private JList<String> userList;
    private DefaultListModel<String> listModel;

    public ChatWindow() {
        // Настройка главного окна
        setTitle("Мессенджер Карета");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Центрирование на экране
        setLayout(new BorderLayout());

        // Главное поле чата (вывод сообщений)
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Боковая панель со списком активных пользователей
        listModel = new DefaultListModel<>();
        userList = new JList<>(listModel);
        userList.setPreferredSize(new Dimension(150, 0));
        
        // Пример заполнения (для теста, потом данные будут идти от сервера)
        listModel.addElement("Общий чат (всем)");
        userList.setSelectedIndex(0); 
        add(new JScrollPane(userList), BorderLayout.EAST);

        // Нижняя панель ввода
        JPanel bottomPanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        JButton sendButton = new JButton("Отправить");

        bottomPanel.add(messageField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        // Обработка отправки по клику или по нажатию Enter
        sendButton.addActionListener(e -> sendMessage());
        messageField.addActionListener(e -> sendMessage());
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            // Оповещаем слушателей (в данном случае Client), что пользователь ввел данные
            for (var listener : listeners) {
                listener.accept(text);
            }
            messageField.setText("");
        }
    }

    @Override
    public void start() {
        // Запуск UI в потоке обработки событий Swing
        SwingUtilities.invokeLater(() -> this.setVisible(true));
    }

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
                    default:
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