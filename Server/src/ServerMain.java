import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.beans.factory.annotation.Autowired;
import ru.gr0946x.net.Server;

@SpringBootApplication
@EntityScan("ru.gr0946x.entity")
@EnableJpaRepositories("ru.gr0946x.repository")
public class ServerMain implements CommandLineRunner {

    @Autowired
    private Server chatServer;

    public static void main(String[] args) {
        // Запускаем Spring Boot приложение
        SpringApplication.run(ServerMain.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        // Этот метод выполнится сразу после старта Spring Boot контекста
        int port = 8081; // Порт нашего сервера чата
        chatServer.start(port);
        
        // Добавляем хук завершения, чтобы корректно закрывать сокеты при остановке приложения
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            chatServer.stop();
        }));
    }
}