package ru.gr0946x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

import ru.gr0946x.net.MessageType;
import ru.gr0946x.net.ProtocolConstants;

public class InteractiveClient {

    // Имя зарегистрированного пользователя
    private static String registeredUsername = null;

    public static void main(String[] args) {

        try (
                Socket socket =
                        new Socket(
                                "localhost",
                                ProtocolConstants.DEFAULT_PORT
                        );

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        );

                BufferedReader in =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        );

                Scanner scanner =
                        new Scanner(System.in)
        ) {

            System.out.println("Подключено к серверу!");

            System.out.println(
                    "Для регистрации просто введи имя."
            );

            System.out.println(
                    "Для отправки сообщения:"
            );

            System.out.println(
                    "MESSAGE:Текст сообщения"
            );

            System.out.println(
                    "Для просмотра истории:"
            );

            System.out.println(
                    "HISTORY:пользователь1:пользователь2"
            );

            // =====================================
            // ПОТОК ЧТЕНИЯ СООБЩЕНИЙ
            // =====================================

            new Thread(() -> {

                try {

                    String incoming;

                    while ((incoming = in.readLine()) != null) {

                        String[] parts = incoming.split(":");

                        // ============================
                        // СООБЩЕНИЯ ЧАТА
                        // ============================

                        if (parts.length >= 4
                                && "MESSAGE".equals(parts[0])) {

                            String msgId = parts[1];

                            String senderName = parts[2];

                            String text = parts[3];

                            System.out.println(
                                    "\n"
                                            + senderName
                                            + ": "
                                            + text
                            );

                            // Подтверждение прочтения
                            out.println("READ:" + msgId);
                        }

                        // ============================
                        // ИСТОРИЯ СООБЩЕНИЙ
                        // ============================

                        else if ("HISTORY".equals(parts[0])) {

                            String sender = parts[1];

                            String text = parts[2];

                            System.out.println(
                                    "[ИСТОРИЯ] "
                                            + sender
                                            + ": "
                                            + text
                            );
                        }

                        // ============================
                        // ОТВЕТЫ СЕРВЕРА
                        // ============================

                        else {

                            System.out.println(
                                    "\n[СЕРВЕР]: "
                                            + incoming
                            );

                            // Успешная регистрация
                            if (incoming.startsWith("INFO:SUCCESS:User ")) {

                                String[] infoParts = incoming.split(":");

                                String raw = infoParts[2];

                                registeredUsername =
                                        raw.replace("User ", "")
                                                .replace(" registered", "")
                                                .trim();

                                System.out.println(
                                        "Вы вошли как: "
                                                + registeredUsername
                                );
                            }

                            // Если пользователь уже существует — тоже логинимся
                            else if (incoming.startsWith("ERROR:User ")
                                    && incoming.contains("already exists")) {

                                String raw =
                                        incoming.replace("ERROR:User ", "")
                                                .replace(" already exists", "")
                                                .trim();

                                registeredUsername = raw;

                                System.out.println(
                                        "Вход выполнен как: "
                                                + registeredUsername
                                );
                            }
                        }

                        System.out.print("> ");
                    }

                } catch (IOException e) {

                    System.out.println(
                            "\nСоединение потеряно."
                    );
                }

            }).start();

            // =====================================
            // ВВОД КОМАНД
            // =====================================

            while (true) {

                System.out.print("> ");

                String input = scanner.nextLine();

                if ("exit".equalsIgnoreCase(input)) {
                    break;
                }

                String request;

                // ============================
                // ОТПРАВКА СООБЩЕНИЯ
                // ============================

                if (input.startsWith("MESSAGE:")) {

                    if (registeredUsername == null) {

                        System.out.println(
                                "Сначала зарегистрируйтесь!"
                        );

                        continue;
                    }

                    String msgText =
                            input.substring(8);

                    request =
                            "MESSAGE"
                                    + ProtocolConstants.COMMAND_SEPARATOR
                                    + registeredUsername
                                    + ProtocolConstants.COMMAND_SEPARATOR
                                    + msgText;
                }

                // ============================
                // ЗАПРОС ИСТОРИИ
                // ============================

                else if (input.startsWith("HISTORY:")) {

                    request = input;
                }

                // ============================
                // РЕГИСТРАЦИЯ
                // ============================

                else {

                    request =
                            MessageType.REQUEST.name()
                                    + ProtocolConstants.COMMAND_SEPARATOR
                                    + "REGISTER"
                                    + ProtocolConstants.COMMAND_SEPARATOR
                                    + input;
                }

                out.println(request);
            }

        } catch (IOException e) {

            System.err.println(
                    "Ошибка клиента: "
                            + e.getMessage()
            );
        }
    }
}