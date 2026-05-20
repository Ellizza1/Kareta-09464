import ru.gr0946x.net.Client;
import ru.gr0946x.ui.ChatWindow;
import ru.gr0946x.ui.Ui;
import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        try {
            var c = new Client("localhost", 9460);
            Ui ui = new ChatWindow(); 
            
            ui.addUserDataListener(c::sendData);
            c.addDataListener(ui::showInfo);
            c.start();
            ui.start();
        } catch (IOException e) {
            System.out.println("Ошибка запуска клиента: " + e.getMessage());
        }
    }
}