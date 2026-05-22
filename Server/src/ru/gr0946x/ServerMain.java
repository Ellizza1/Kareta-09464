package ru.gr0946x;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import ru.gr0946x.entity.Message;
import ru.gr0946x.entity.MessageStatus;
import ru.gr0946x.entity.User;
import ru.gr0946x.net.Communicator;
import ru.gr0946x.net.ProtocolConstants;
import ru.gr0946x.repository.MessageRepository;
import ru.gr0946x.repository.UserRepository;

import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

@SpringBootApplication(scanBasePackages = "ru.gr0946x")
public class ServerMain {

    private final List<Communicator> clients = new CopyOnWriteArrayList<>();
    private final Map<String, Communicator> userConnections =
        new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.setProperty("spring.classformat.ignore", "true");
        SpringApplication.run(ServerMain.class, args);
    }

    @Bean
    public CommandLineRunner runServer(
            UserRepository userRepository,
            MessageRepository messageRepository
    ) {

        return args -> {

            new Thread(() -> {

                try (ServerSocket serverSocket =
                             new ServerSocket(ProtocolConstants.DEFAULT_PORT)) {

                    System.out.println(
                            "Сервер запущен на порту "
                                    + ProtocolConstants.DEFAULT_PORT
                    );

                    while (true) {

                        Socket clientSocket = serverSocket.accept();

                        Communicator comm =
                                new Communicator(clientSocket);

                        clients.add(comm);

                        comm.addDataListener(data -> {

                            System.out.println("Получено: " + data);

                            String[] parts =
                                    data.split(
                                            ProtocolConstants.COMMAND_SEPARATOR
                                    );

                            if (parts.length == 0) {
                                return;
                            }

                            String commandType = parts[0];

                            // =====================================
                            // РЕГИСТРАЦИЯ
                            // =====================================

                            if ("REGISTER".equals(commandType)) {
                                if (parts.length >= 3) {

                                    String username = parts[1];

                                    String password = parts[2];

                                    // Имя должно начинаться с буквы
                                    if (!username.matches("^[a-zA-Zа-яА-Я].*")) {

                                        comm.sendData(
                                                "ERROR:Username must start with a letter"
                                        );

                                        return;
                                    }

                                    if (userRepository
                                            .findByUsernameIgnoreCase(username)
                                            .isPresent()) {

                                        comm.sendData(
                                                "ERROR:User already exists"
                                        );

                                    } else {

                                        User user =
                                                new User(username, password);

                                        userRepository.save(user);

                                        userConnections.put(
                                                username.toLowerCase(),
                                                comm
                                        );

                                        broadcastUsers();

                                        comm.sendData(
                                                "REGISTER_SUCCESS:"
                                                        + username
                                        );
                                    }
                                }
                            }
                            else if ("LOGIN".equals(commandType)) {

                                if (parts.length >= 3) {

                                    String username = parts[1];

                                    String password = parts[2];

                                    User user =
                                            userRepository
                                                    .findByUsernameIgnoreCase(username)
                                                    .orElse(null);

                                    if (user == null) {

                                        comm.sendData(
                                                "ERROR:User not found"
                                        );

                                    } else if (!user.getPassword()
                                            .equals(password)) {

                                        comm.sendData(
                                                "ERROR:Wrong password"
                                        );

                                    } else {

                                        userConnections.put(
                                                username.toLowerCase(),
                                                comm
                                        );

                                        broadcastUsers();

                                        comm.sendData(
                                                "LOGIN_SUCCESS:"
                                                        + username
                                        );

                                        // =====================================
                                        // НЕПРОЧИТАННЫЕ ЛИЧНЫЕ
                                        // =====================================

                                        List<Message> unreadMessages =
                                                messageRepository
                                                        .findByRecipientUsernameIgnoreCaseAndStatus(
                                                                username,
                                                                MessageStatus.SENT
                                                        );

                                        for (Message msg : unreadMessages) {

                                            comm.sendData(
                                                    "PRIVATE:"
                                                            + msg.getId()
                                                            + ":"
                                                            + msg.getSender().getUsername()
                                                            + ":"
                                                            + msg.getText()
                                            );
                                        }

                                        // =====================================
                                        // НЕПРОЧИТАННЫЕ ОБЩИЕ
                                        // =====================================

                                        List<Message> unreadGlobal =
                                                messageRepository
                                                        .findByRecipientIsNullAndStatus(
                                                                MessageStatus.SENT
                                                        );

                                        for (Message msg : unreadGlobal) {

                                            // не отправляем автору
                                            if (!msg.getSender()
                                                    .getUsername()
                                                    .equalsIgnoreCase(username)) {

                                                comm.sendData(
                                                        "MESSAGE:"
                                                                + msg.getId()
                                                                + ":"
                                                                + msg.getSender().getUsername()
                                                                + ":"
                                                                + msg.getText()
                                                );
                                            }
                                        }


                                    }

                                }
                            }


                          else if ("MESSAGE_ALL".equals(commandType)) {

                                if (parts.length >= 3) {

                                    String senderName = parts[1];

                                    String msgText = parts[2];

                                    User sender =
                                            userRepository
                                                    .findByUsernameIgnoreCase(senderName)
                                                    .orElse(null);

                                    if (sender != null) {

                                        Message message =
                                                new Message(sender, null, msgText);

                                        message.setSentAt(LocalDateTime.now());

                                        Message savedMessage =
                                                messageRepository.save(message);

                                        Long messageId =
                                                savedMessage.getId();

                                        // Всем клиентам
                                        for (Communicator client : clients) {

                                            client.sendData(
                                                    "MESSAGE:"
                                                            + messageId
                                                            + ":"
                                                            + senderName
                                                            + ":"
                                                            + msgText
                                            );
                                        }
                                    }

                                }
                            }

                            // =====================================
                            // ЛИЧНОЕ СООБЩЕНИЕ
                            // =====================================

                            else if ("MESSAGE_PRIVATE".equals(commandType)) {

                                if (parts.length >= 4) {

                                    String senderName = parts[1];

                                    String recipientName = parts[2];

                                    String msgText = parts[3];

                                    User sender =
                                            userRepository
                                                    .findByUsernameIgnoreCase(senderName)
                                                    .orElse(null);

                                    User recipient =
                                            userRepository
                                                    .findByUsernameIgnoreCase(recipientName)
                                                    .orElse(null);

                                    if (sender != null && recipient != null) {

                                        Message message =
                                                new Message(sender, recipient, msgText);

                                        message.setSentAt(LocalDateTime.now());

                                        Message savedMessage =
                                        messageRepository.save(message);

                                        Long messageId =
                                            savedMessage.getId();

                                        Communicator recipientComm =
                                                userConnections.get(
                                                        recipientName.toLowerCase()
                                                );

                                        // Отправка получателю
                                        if (recipientComm != null) {

                                            recipientComm.sendData(
                                                "PRIVATE:"
                                                        + messageId
                                                        + ":"
                                                        + senderName
                                                        + ":"
                                                        + msgText
                                            );
                                        }

                                    }
                                }
                            }
                            

                            // =====================================
                            // ИСТОРИЯ СООБЩЕНИЙ
                            // =====================================

                            else if ("HISTORY".equals(commandType)) {

                                if (parts.length >= 3) {

                                    String user1 = parts[1];

                                    String user2 = parts[2];

                                    List<Message> history =
                                            messageRepository
                                                    .findChatHistory(
                                                            user1,
                                                            user2
                                                    );

                                    int limit =
                                            Math.min(history.size(), 10);

                                    for (int i = limit - 1;
                                         i >= 0;
                                         i--) {

                                        Message msg = history.get(i);

                                        comm.sendData(
                                                "HISTORY:"
                                                        + msg.getSender()
                                                        .getUsername()
                                                        + ":"
                                                        + msg.getText()
                                        );
                                    }
                                }
                            }
                            
                            else if ("READ".equals(commandType)) {
                                if (parts.length >= 2) {

                                    Long msgId =
                                            Long.parseLong(parts[1]);

                                    Message msg =
                                            messageRepository
                                                    .findById(msgId)
                                                    .orElse(null);

                                    if (msg != null) {

                                        msg.setStatus(
                                                MessageStatus.READ
                                        );

                                        messageRepository.save(msg);

                                        System.out.println(
                                                "Сообщение "
                                                        + msgId
                                                        + " прочитано"
                                        );
                                    }
                                }
                            }




                            else if ("SEARCH".equals(commandType)) {

                                if (parts.length >= 2) {

                                    String query = parts[1];

                                    List<Message> results =
                                            messageRepository
                                                    .findByTextContainingIgnoreCase(query);

                                    for (Message msg : results) {

                                        String sender =
                                                msg.getSender()
                                                        .getUsername();

                                        String text =
                                                msg.getText();

                                        comm.sendData(
                                                "SEARCH_RESULT:"
                                                        + sender
                                                        + ":"
                                                        + text
                                        );
                                    }
                                }
                            }


                        });

                        comm.start();
                        new Thread(() -> {
                            try {

                                while (!clientSocket.isClosed()) {
                                    Thread.sleep(1000);
                                }

                            } catch (Exception ignored) {
                            }

                            clients.remove(comm);

                            userConnections.values().remove(comm);

                            broadcastUsers();

                        }).start();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        };
    }

    // =====================================
    // DATABASE
    // =====================================

    @Bean
    public DataSource dataSource() {

        DriverManagerDataSource dataSource =
                new DriverManagerDataSource();

        dataSource.setDriverClassName(
                "org.postgresql.Driver"
        );

        dataSource.setUrl(
                "jdbc:postgresql://localhost:5432/careta_db"
        );

        dataSource.setUsername("emilakmaev");

        dataSource.setPassword("");

        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean
    entityManagerFactory(DataSource dataSource) {

        LocalContainerEntityManagerFactoryBean em =
                new LocalContainerEntityManagerFactoryBean();

        em.setDataSource(dataSource);

        em.setPackagesToScan("ru.gr0946x.entity");

        em.setJpaVendorAdapter(
                new HibernateJpaVendorAdapter()
        );

        java.util.Properties props =
                new java.util.Properties();

        props.setProperty(
                "hibernate.hbm2ddl.auto",
                "update"
        );

        em.setJpaProperties(props);

        return em;
    }

    private void broadcastUsers() {

        StringBuilder users = new StringBuilder();

        for (String username : userConnections.keySet()) {

            users.append(username).append(",");
        }

        String usersMessage =
                "USERS:" + users;

        for (Communicator client : clients) {

            client.sendData(usersMessage);
        }
    }
}