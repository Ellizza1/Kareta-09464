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

                            // 1. ЛОГИКА РЕГИСТРАЦИИ (REQUEST:REGISTER:имя)
                            if ("REQUEST".equals(commandType)) {
                                if (parts.length >= 3 && "REGISTER".equals(parts[1])) {
                                    String username = parts[2];
                                    
                                    // Использование регистронезависимого поиска
                                    if (userRepository.findByUsernameIgnoreCase(username).isPresent()) {
                                        comm.sendData("ERROR:User " + username + " already exists");
                                    } else {
                                        userRepository.save(new User(username, "default_password"));
                                        comm.sendData("INFO:SUCCESS:User " + username + " registered");
                                    }
                                }
                            } 
                            // 2. ЛОГИКА ЧАТА (MESSAGE:текст)
                            else if ("MESSAGE".equals(commandType)) {
                                if (parts.length >= 2) {
                                    String msgText = parts[1];
                                    
                                    // Примечание: Для полноценного сохранения отправителя, 
                                    // нужно сохранять имя пользователя в объекте Communicator 
                                    // при регистрации и извлекать его здесь.
                                    
                                    for (Communicator client : clients) {
                                        client.sendData("MESSAGE:Broadcast: " + msgText);
                                    }
                                }
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