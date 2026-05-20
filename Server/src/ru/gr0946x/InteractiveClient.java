package ru.gr0946x;

import java.io.*;
import java.net.*;
import java.util.Scanner;
import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

public class InteractiveClient {
    // Переменная для хранения имени, полученного после успешной регистрации
    private static String registeredUsername = null;

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", ProtocolConstants.DEFAULT_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             Scanner scanner = new Scanner(System.in)) {

            System.out.println("Подключено к серверу!");
            System.out.println("Для регистрации просто введи имя.");
            System.out.println("Для отправки сообщения введи: MESSAGE:Твой текст");

            new Thread(() -> {
                try {
                    String incoming;
                    while ((incoming = in.readLine()) != null) {
                        String[] parts = incoming.split(":");
                        
                        // Обработка сообщений чата
                        if (parts.length >= 4 && "MESSAGE".equals(parts[0])) {
                            String msgId = parts[1];
                            String senderName = parts[2];
                            String text = parts[3];
                            System.out.println("\n" + senderName + ": " + text);
                            out.println("READ:" + msgId);
                        } 
                        // Обработка ответа сервера (например, успех регистрации)
                        else {
                            System.out.println("\n[СЕРВЕР]: " + incoming);
                            // Если сервер подтвердил регистрацию, запоминаем имя
                            if (incoming.startsWith("INFO:SUCCESS:User ")) {
                                    String[] infoParts = incoming.split(":");
                                    String raw = infoParts[2];
                                    System.out.println("DEBUG: Исходная строка имени: [" + raw + "]");
                                    registeredUsername = raw.replace("User ", "").replace(" registered", "").trim(); // добавил trim()
                                    System.out.println("DEBUG: Имя после парсинга: [" + registeredUsername + "]");
                                }
                        }
                        System.out.print("> ");
                    }
                } catch (IOException e) {
                    System.out.println("\nСоединение потеряно.");
                }
            }).start();

            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) break;

                String request;
                if (input.startsWith("MESSAGE:")) {
                    if (registeredUsername == null) {
                        System.out.println("Сначала зарегистрируйтесь!");
                        continue;
                    }
                    // ОТПРАВЛЯЕМ: MESSAGE:ИМЯ:ТЕКСТ
                    String msgText = input.substring(8);
                    request = "MESSAGE" + ProtocolConstants.COMMAND_SEPARATOR + registeredUsername + ProtocolConstants.COMMAND_SEPARATOR + msgText;
                } else {
                    request = MessageType.REQUEST.name() + ProtocolConstants.COMMAND_SEPARATOR + 
                              "REGISTER" + ProtocolConstants.COMMAND_SEPARATOR + input;
                }
                out.println(request);
            }
        } catch (IOException e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        }
    }
}