package ru.gr0946x.net;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class Client {
    private Communicator communicator;
    private final List<BiConsumer<String, MessageType>> listeners = new ArrayList<>();

    public Client(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        communicator = new Communicator(socket);
        // Подписываемся на сырые данные из сети
        communicator.addDataListener(this::parseIncomingData);
    }

    public void start() {
        communicator.start();
    }

    public void sendData(String data) {
        if (communicator != null) {
            communicator.sendData(data);
        }
    }

    // Этот метод разбивает строку от сервера вида "REQUEST#Введите имя:" на тип и текст
    private void parseIncomingData(String data) {
        try {
            var fullInfo = data.split(ProtocolConstants.COMMAND_SEPARATOR, 2);
            if (fullInfo.length == 2) {
                MessageType type = MessageType.valueOf(fullInfo[0]);
                String clearText = fullInfo[1];
                
                // Передаем в UI отдельно чистый текст и отдельно тип сообщения
                notifyListeners(clearText, type);
            } else {
                // Если разделителя нет, считаем это обычным сообщением
                notifyListeners(data, MessageType.MESSAGE);
            }
        } catch (Exception e) {
            // В случае ошибки парсинга выводим как системное сообщение
            notifyListeners(data, MessageType.INFO);
        }
    }

    public void addDataListener(BiConsumer<String, MessageType> listener) {
        listeners.add(listener);
    }

    public void removeDataListener(BiConsumer<String, MessageType> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String data, MessageType type) {
        for (var listener : listeners) {
            listener.accept(data, type);
        }
    }
}