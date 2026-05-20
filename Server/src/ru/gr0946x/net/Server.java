package ru.gr0946x.net;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.gr0946x.repository.UserRepository;
import ru.gr0946x.repository.MessageRepository;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import ru.gr0946x.net.Communicator;

@Component
public class Server {
    private final List<ConnectedClient> clients = new ArrayList<>();
    private ServerSocket serverSocket;
    private boolean isRunning = false;

    // Внедряем репозитории Spring Data
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MessageRepository messageRepository;

    public void start(int port) {
        try {
            serverSocket = new ServerSocket(port);
            isRunning = true;
            System.out.println("Сервер чата запущен на порту " + port);

            // Запускаем поток приема подключений клиентов
            new Thread(() -> {
                while (isRunning) {
                    try {
                        Socket socket = serverSocket.accept();
                        System.out.println("Клиент подключился: " + socket.getInetAddress());
                        
                        // Передаем репозитории в конструктор клиента
                        ConnectedClient client = new ConnectedClient(socket, this, userRepository, messageRepository);
                        new Thread(client).start();
                    } catch (IOException e) {
                        if (isRunning) {
                            System.out.println("Ошибка при приеме подключения: " + e.getMessage());
                        }
                    }
                }
            }).start();

        } catch (IOException e) {
            System.out.println("Не удалось запустить сервер: " + e.getMessage());
        }
    }

    public synchronized void addClient(ConnectedClient client) {
        clients.add(client);
    }

    public synchronized void removeClient(ConnectedClient client) {
        clients.remove(client);
    }

    public synchronized void sendForAll(String data) {
        for (ConnectedClient client : clients) {
            client.sendData(data);
        }
    }

    public void stop() {
        isRunning = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            synchronized (this) {
                for (ConnectedClient client : clients) {
                    client.close();
                }
                clients.clear();
            }
            System.out.println("Сервер остановлен.");
        } catch (IOException e) {
            System.out.println("Ошибка при остановке сервера: " + e.getMessage());
        }
    }
}