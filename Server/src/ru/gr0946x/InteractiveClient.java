package ru.gr0946x;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

public class InteractiveClient {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", ProtocolConstants.DEFAULT_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Подключено к серверу! Введи имя для регистрации:");

            while (true) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) break;

                // Формируем сообщение: ТИП:КОМАНДА:ИМЯ
                String request = MessageType.REQUEST.name() + ProtocolConstants.COMMAND_SEPARATOR + 
                                 "REGISTER" + ProtocolConstants.COMMAND_SEPARATOR + input;
                
                out.println(request);
                System.out.println("Ответ сервера: " + in.readLine());
            }
        } catch (IOException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        }
    }
}