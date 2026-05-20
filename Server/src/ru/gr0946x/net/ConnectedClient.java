package ru.gr0946x.net;

import ru.gr0946x.entity.User;
import ru.gr0946x.entity.Message;
import ru.gr0946x.repository.UserRepository;
import ru.gr0946x.repository.MessageRepository;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ConnectedClient implements Runnable {
    private final Socket socket;
    private final Communicator communicator;
    private final Server server;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    
    private User dbUser; // Объект пользователя из БД
    private String name;

    // Добавляем throws IOException в заголовок конструктора
    public ConnectedClient(Socket socket, Server server, UserRepository userRepository, MessageRepository messageRepository) throws IOException {
        this.socket = socket;
        this.server = server;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;

        // Теперь компилятор спокоен: если здесь будет ошибка,
        // создание объекта прервется, и final-поле не останется сломанным
        this.communicator = new Communicator(socket);

        this.communicator.addDataListener(this::parseIncomingData);
    }

    public void start() {
        communicator.start();
        // Сразу запрашиваем имя при подключении
        sendData(MessageType.REQUEST + ProtocolConstants.COMMAND_SEPARATOR + "Введите имя:");
    }

    @Override
    public void run() {
        start();
    }

    private void parseIncomingData(String data) {
        // Если имя еще не установлено — значит, пришел ответ на запрос имени
        if (name == null) {
            String trimmedName = data.trim();
            if (trimmedName.isEmpty()) {
                sendData(MessageType.ERROR + ProtocolConstants.COMMAND_SEPARATOR + "Имя не может быть пустым!");
                sendData(MessageType.REQUEST + ProtocolConstants.COMMAND_SEPARATOR + "Введите имя:");
                return;
            }

            // Ищем пользователя в БД или создаем нового (Регистрация / Авторизация)
            this.dbUser = userRepository.findByUsername(trimmedName).orElseGet(() -> {
                User newUser = new User(trimmedName, "default_password"); // По ТЗ можно сделать фиксированный или добавить шаг пароля
                return userRepository.save(newUser);
            });

            this.name = dbUser.getUsername();
            server.addClient(this);

            // Отправляем пользователю приветствие
            sendData(MessageType.INFO + ProtocolConstants.COMMAND_SEPARATOR + "Вы успешно вошли как " + name);

            // ЗАГРУЗКА ИСТОРИИ ИЗ БД: достаем старые сообщения и отправляем только этому клиенту
            List<Message> history = messageRepository.findByRecipientIsNullOrderBySentAtAsc();
            for (Message msg : history) {
                sendData(MessageType.MESSAGE + ProtocolConstants.COMMAND_SEPARATOR + msg.getSender().getUsername() + ": " + msg.getText());
            }

            // Оповещаем всех остальных, что юзер вошел
            sendForAll(MessageType.INFO, "Пользователь " + name + " вошел в чат");
        } else {
            // Если имя уже есть — обрабатываем как обычное сообщение в чат
            
            // Сначала сохраняем сообщение в базу данных!
            Message newMessage = new Message(this.dbUser, null, data);
            messageRepository.save(newMessage);

            // Затем рассылаем всем активным участникам
            sendForAll(MessageType.MESSAGE, data);
        }
    }

    public void sendData(String data) {
        communicator.sendData(data);
    }

    private void sendForAll(MessageType type, String data) {
        var author = (type == MessageType.MESSAGE) ? name + ProtocolConstants.AUTHOR_SEPARATOR : "";
        server.sendForAll(type + ProtocolConstants.COMMAND_SEPARATOR + author + data);
    }

    public String getName() {
        return name;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("Ошибка закрытия сокета: " + e.getMessage());
        }
    }
}