package fr.insa.crypto.mail;

import java.util.Scanner;

import javax.mail.Session;

import fr.insa.crypto.utils.Logger;

public class Test {
    public static void main(String[] args) {
        Logger.info("Starting email application...");

        Scanner scanner = new Scanner(System.in);
        Authentication auth = null;
        Session session = null;

        while (true) {
            System.out.println("\n===== EMAIL APPLICATION =====");
            System.out.println("1. Login");
            System.out.println("2. Send Email");
            System.out.println("3. Logout");
            System.out.println("4. Exit");
            System.out.print("Enter your choice: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1": // Login
                    if (auth != null) {
                        System.out.println("Already logged in. Please logout first.");
                        break;
                    }
                    System.out.print("Enter your email: ");
                    String email = scanner.nextLine();
                    System.out.print("Enter your app key/password: ");
                    String appKey = scanner.nextLine();

                    System.out.print("Use SSL instead of TLS? (y/n): ");
                    boolean useSSL = scanner.nextLine().toLowerCase().startsWith("y");

                    auth = new Authentication(email, appKey, useSSL);
                    session = auth.getAuthenticatedSession();

                    if (session != null) {
                        Logger.info("Email authentication successful!");
                    } else {
                        Logger.error("Email authentication failed!");
                        auth = null;
                    }
                    break;

                case "2": // Send Email
                    if (auth == null || session == null) {
                        Logger.error("You must login first!");
                    } else {
                        System.out.print("Enter recipient email: ");
                        String toEmail = scanner.nextLine();

                        System.out.print("Enter subject: ");
                        String subject = scanner.nextLine();

                        System.out.print("Enter email body: ");
                        String body = scanner.nextLine();

                        MailSender.sendEmail(session, toEmail, subject, body);
                    }
                    break;

                case "3": // Logout
                    if (auth != null) {
                        auth.logout();
                        auth = null;
                        session = null;
                        Logger.info("Logged out successfully.");
                    } else {
                        Logger.info("Not currently logged in.");
                    }
                    break;

                case "4": // Exit
                    Logger.info("Exiting application...");
                    scanner.close();
                    System.exit(0);
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
