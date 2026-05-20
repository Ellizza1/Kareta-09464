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
                    "Регистрация:"
            );

            System.out.println(
                    "REGISTER:логин:пароль"
            );

            System.out.println(
                    "Вход:"
            );

            System.out.println(
                    "LOGIN:логин:пароль"
            );

            System.out.println(
                    "Общий чат:"
            );

            System.out.println(
                    "ALL:текст"
            );

            System.out.println(
                    "Личное сообщение:"
            );

            System.out.println(
                    "PRIVATE:пользователь:текст"
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

                        if (parts.length >= 3
                                && "MESSAGE".equals(parts[0])) {

                            String senderName = parts[1];

                            String text = parts[2];

                            System.out.println(
                                    "\n[ОБЩИЙ ЧАТ] "
                                            + senderName
                                            + ": "
                                            + text
                            );
                        }
                        else if (parts.length >= 3
                                && "PRIVATE".equals(parts[0])) {

                            String senderName = parts[1];

                            String text = parts[2];

                            System.out.println(
                                    "\n[ЛИЧНОЕ] "
                                            + senderName
                                            + ": "
                                            + text
                            );
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
                        else if ("USERS".equals(parts[0])) {
                            String users =
                                    incoming.replace("USERS:", "");

                            System.out.println(
                                    "\n[ОНЛАЙН] " + users
                            );
                        }

                        // ============================
                        // ОТВЕТЫ СЕРВЕРА
                        // ============================

                        else {
                            if (incoming.startsWith(
                                    "REGISTER_SUCCESS:"
                            )) {

                                registeredUsername =
                                        incoming.replace(
                                                "REGISTER_SUCCESS:",
                                                ""
                                        );

                                System.out.println(
                                        "Регистрация успешна: "
                                                + registeredUsername
                                );
                            }

                            else if (incoming.startsWith(
                                    "LOGIN_SUCCESS:"
                            )) {

                                registeredUsername =
                                        incoming.replace(
                                                "LOGIN_SUCCESS:",
                                                ""
                                        );

                                System.out.println(
                                        "Вход выполнен: "
                                                + registeredUsername
                                );
                            }
                                // остальные ответы сервера
                            else {

                                System.out.println(
                                        "\n[СЕРВЕР]: "
                                                + incoming
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
                if (input.startsWith("ALL:")) {

                    if (registeredUsername == null) {

                        System.out.println(
                                "Сначала зарегистрируйтесь!"
                        );

                        continue;
                    }

                    String msgText = input.substring(4);

                    request =
                            "MESSAGE_ALL:"
                                    + registeredUsername
                                    + ":"
                                    + msgText;
                }

                // Личное сообщение
                else if (input.startsWith("PRIVATE:")) {

                    if (registeredUsername == null) {

                        System.out.println(
                                "Сначала зарегистрируйтесь!"
                        );

                        continue;
                    }

                    // PRIVATE:lll:привет
                    request =
                            "MESSAGE_PRIVATE:"
                                    + registeredUsername
                                    + ":"
                                    + input.substring(8);
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

                else if (input.startsWith("REGISTER:")) {

                    String[] regParts =
                            input.split(":", 3);

                    if (regParts.length < 3) {

                        System.out.println(
                                "Формат: REGISTER:логин:пароль"
                        );

                        continue;
                    }

                    request =
                            "REGISTER:"
                                    + regParts[1]
                                    + ":"
                                    + regParts[2];

                    out.println(request);
                }

                else if (input.startsWith("LOGIN:")) {

                    String[] loginParts =
                            input.split(":", 3);

                    if (loginParts.length < 3) {

                        System.out.println(
                                "Формат: LOGIN:логин:пароль"
                        );

                        continue;
                    }

                    request =
                            "LOGIN:"
                                    + loginParts[1]
                                    + ":"
                                    + loginParts[2];

                    out.println(request);
                }

                else {

                    System.out.println(
                            "Неизвестная команда!"
                    );

                    System.out.println(
                            "REGISTER:логин:пароль"
                    );

                    System.out.println(
                            "LOGIN:логин:пароль"
                    );

                    System.out.println(
                            "ALL:текст"
                    );

                    System.out.println(
                            "PRIVATE:пользователь:текст"
                    );

                    System.out.println(
                            "HISTORY:user1:user2"
                    );

                    continue;
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