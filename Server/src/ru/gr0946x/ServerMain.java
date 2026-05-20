package ru.gr0946x;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import ru.gr0946x.entity.User;
import ru.gr0946x.net.Communicator;
import ru.gr0946x.net.ProtocolConstants;
import ru.gr0946x.repository.UserRepository;

import java.net.ServerSocket;
import java.net.Socket;
import javax.sql.DataSource;

@SpringBootApplication(scanBasePackages = "ru.gr0946x")
public class ServerMain {

    public static void main(String[] args) {
        System.setProperty("spring.classformat.ignore", "true");
        SpringApplication.run(ServerMain.class, args);
    }

    @Bean
    public CommandLineRunner runServer(UserRepository userRepository) {
        return args -> {
            new Thread(() -> {
                try (ServerSocket serverSocket = new ServerSocket(ProtocolConstants.DEFAULT_PORT)) {
                    System.out.println("Сервер запущен на порту " + ProtocolConstants.DEFAULT_PORT);
                    
                    while (true) {
                        Socket clientSocket = serverSocket.accept();
                        Communicator comm = new Communicator(clientSocket);
                        
                        comm.addDataListener(data -> {
                            System.out.println("Получено: " + data);
                            String[] parts = data.split(ProtocolConstants.COMMAND_SEPARATOR);
                            
                            // Проверка: REQUEST:REGISTER:Имя
                            if (parts.length >= 3 && "REQUEST".equals(parts[0]) && "REGISTER".equals(parts[1])) {
                                String username = parts[2];
                                
                                if (userRepository.findByUsername(username).isPresent()) {
                                    comm.sendData("ERROR:User " + username + " already exists");
                                } else {
                                    userRepository.save(new User(username, "default_password"));
                                    comm.sendData("INFO:SUCCESS:User " + username + " registered");
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
        // Добавим автоматическое создание таблиц при запуске
        java.util.Properties props = new java.util.Properties();
        props.setProperty("hibernate.hbm2ddl.auto", "update");
        em.setJpaProperties(props);
        return em;
    }
}