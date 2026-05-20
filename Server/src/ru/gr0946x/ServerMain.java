package ru.gr0946x;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import ru.gr0946x.entity.Message;
import ru.gr0946x.entity.User;
import ru.gr0946x.entity.MessageStatus;
import ru.gr0946x.net.Communicator;
import ru.gr0946x.net.ProtocolConstants;
import ru.gr0946x.repository.MessageRepository;
import ru.gr0946x.repository.UserRepository;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.sql.DataSource;

@SpringBootApplication(scanBasePackages = "ru.gr0946x")
public class ServerMain {

    private final List<Communicator> clients = new CopyOnWriteArrayList<>();

    public static void main(String[] args) {
        System.setProperty("spring.classformat.ignore", "true");
        SpringApplication.run(ServerMain.class, args);
    }

    @Bean
    public CommandLineRunner runServer(UserRepository userRepository, MessageRepository messageRepository) {
        return args -> {
            new Thread(() -> {
                try (ServerSocket serverSocket = new ServerSocket(ProtocolConstants.DEFAULT_PORT)) {
                    System.out.println("Сервер запущен на порту " + ProtocolConstants.DEFAULT_PORT);
                    
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        Communicator comm = new Communicator(clientSocket);
                        clients.add(comm);
                        
                        comm.addDataListener(data -> {
                            System.out.println("Получено: " + data);
                            String[] parts = data.split(ProtocolConstants.COMMAND_SEPARATOR);
                            if (parts.length == 0) return;
                            String commandType = parts[0];

                            // 1. РЕГИСТРАЦИЯ
                            // Блок в ServerMain.java
                            if ("REQUEST".equals(commandType) && "REGISTER".equals(parts[1])) {
                                String username = parts[2];
                                if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
                                    System.out.println("DEBUG: Отправляю ошибку клиенту...");
                                    comm.sendData("ERROR:User " + username + " already exists");
                                } else {
                                    userRepository.save(new User(username, "default_password"));
                                    comm.setUsername(username); 
                                    System.out.println("DEBUG: Отправляю успех клиенту...");
                                    comm.sendData("INFO:SUCCESS:User " + username + " registered");
                                }
                            }
                            // 2. ОТПРАВКА СООБЩЕНИЯ
                            // В ServerMain.java внутри блока MESSAGE:
                            else if ("MESSAGE".equals(commandType)) {
                                // Теперь ожидаем формат: MESSAGE:ИМЯ:ТЕКСТ
                                System.out.println("DEBUG: Длина частей: " + parts.length);
                                if (parts.length >= 3) {
                                    String username = parts[1]; // Берем имя прямо из сообщения
                                    String msgText = parts[2];
                                    
                                    User sender = userRepository.findByUsername(username).orElse(null);
                                    if (sender != null) {
                                        Message savedMessage = messageRepository.save(new Message(sender, null, msgText));
                                        for (Communicator client : clients) {
                                            client.sendData("MESSAGE:" + savedMessage.getId() + ":" + username + ":" + msgText);
                                        }
                                    } else {
                                        comm.sendData("ERROR:Пользователь не найден. Зарегистрируйтесь.");
                                    }
                                }else{
                                    System.out.println("DEBUG: Ошибка формата сообщения! Ожидалось >= 3, получено: " + parts.length);
                                }
                            }
                            // 3. ПОДТВЕРЖДЕНИЕ ПРОЧТЕНИЯ
                            else if ("READ".equals(commandType) && parts.length >= 2) {
                                Long msgId = Long.parseLong(parts[1]);
                                messageRepository.findById(msgId).ifPresent(msg -> {
                                    msg.setStatus(MessageStatus.READ);
                                    messageRepository.save(msg);
                                });
                            }
                        });
                        comm.start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        };
    }
        @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl("jdbc:postgresql://localhost:5432/careta_db");
        dataSource.setUsername("emilakmaev");
        dataSource.setPassword("");
        return dataSource;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("ru.gr0946x.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        java.util.Properties props = new java.util.Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        em.setJpaProperties(props);
        return em;
    }
}