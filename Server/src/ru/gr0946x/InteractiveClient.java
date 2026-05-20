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

            System.out.println("Подключено к серверу!");
            System.out.println("Для регистрации просто введи имя.");
            System.out.println("Для отправки сообщения введи: MESSAGE:Твой текст");

            // ПОТОК ДЛЯ ЧТЕНИЯ СООБЩЕНИЙ ОТ СЕРВЕРА
            // Это позволит получать сообщения от других в реальном времени
            new Thread(() -> {
                try {
                    String incoming;
                    while ((incoming = in.readLine()) != null) {
                        System.out.println("\n[СЕРВЕР]: " + incoming);
                        System.out.print("> "); // просто для удобства ввода
                    }
                } catch (IOException e) {
                    System.out.println("\nСоединение с сервером потеряно.");
                }
            }).start();

            // ОСНОВНОЙ ЦИКЛ ОТПРАВКИ
            while (true) {
                System.out.print("> ");
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) break;

                String request;
                // Проверяем, является ли ввод командой сообщения
                if (input.startsWith("MESSAGE:")) {
                    request = input; 
                } else {
                    // Иначе считаем это запросом на регистрацию
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