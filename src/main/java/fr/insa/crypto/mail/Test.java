package fr.insa.crypto.mail;

import java.util.List;
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
            System.out.println("5. Send Email with Attachment");
            System.out.println("6. View Received Emails");
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

                case "5": // Send Email with Attachment
                    if (auth == null || session == null) {
                        Logger.error("You must login first!");
                    } else {
                        try {
                            System.out.print("Enter recipient email: ");
                            String toEmail = scanner.nextLine();

                            System.out.print("Enter subject: ");
                            String subject = scanner.nextLine();

                            System.out.print("Enter email body: ");
                            String body = scanner.nextLine();
                            
                            // Create attachment handler
                            AttachmentHandler attachmentHandler = new AttachmentHandler();
                            
                            boolean addMoreAttachments = true;
                            while (addMoreAttachments) {
                                System.out.print("Enter file path to attach: ");
                                String filePath = scanner.nextLine();
                                
                                try {
                                    attachmentHandler.addAttachment(filePath);
                                    System.out.println("Attachment added successfully: " + filePath);
                                } catch (Exception e) {
                                    Logger.error("Failed to add attachment: " + e.getMessage());
                                }
                                
                                System.out.print("Add another attachment? (y/n): ");
                                addMoreAttachments = scanner.nextLine().toLowerCase().startsWith("y");
                            }
                            
                            // Send email with attachments
                            MailSender.sendEmailWithAttachments(session, toEmail, subject, body, attachmentHandler);
                            
                        } catch (Exception e) {
                            Logger.error("Error sending email with attachment: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    break;

                case "6": // View Received Emails
                    if (auth == null || session == null) {
                        Logger.error("You must login first!");
                    } else {
                        try {
                            System.out.println("\n===== VIEWING RECEIVED EMAILS =====");
                            System.out.print("Enter mail server (e.g., imap.gmail.com): ");
                            String host = scanner.nextLine();
                            
                            // Si aucun serveur n'est indiqué, utiliser gmail par défaut
                            if (host == null || host.trim().isEmpty()) {
                                host = "imap.gmail.com";
                                System.out.println("Using default server: " + host);
                            }
                            
                            // Créer un MailReceiver avec les identifiants déjà utilisés
                            MailReceiver mailReceiver = new MailReceiver(host, auth.getEmail(), auth.getProperties().getProperty("mail.smtp.user.password") != null ? 
                                                                     auth.getProperties().getProperty("mail.smtp.user.password") : auth.getProperties().getProperty("password.key"));
                            
                            if (!mailReceiver.connect()) {
                                System.out.println("Échec de connexion au serveur de réception. Veuillez réessayer.");
                                break;
                            }
                            
                            System.out.println("1. Voir tous les messages");
                            System.out.println("2. Voir uniquement les messages non lus");
                            System.out.print("Choisissez une option: ");
                            String viewOption = scanner.nextLine();
                            
                            List<MailReceiver.EmailMessage> emails = null;
                            switch (viewOption) {
                                case "1":
                                    System.out.println("\nRécupération de tous les messages...");
                                    emails = mailReceiver.getInboxMessages();
                                    break;
                                case "2":
                                    System.out.println("\nRécupération des messages non lus...");
                                    emails = mailReceiver.getUnreadMessages();
                                    break;
                                default:
                                    System.out.println("Option non reconnue. Affichage de tous les messages.");
                                    emails = mailReceiver.getInboxMessages();
                            }
                            
                            if (emails.isEmpty()) {
                                System.out.println("Aucun message trouvé.");
                            } else {
                                System.out.println("\n===== MAILS REÇUS =====");
                                System.out.println("Nombre de messages: " + emails.size());
                                
                                for (int i = 0; i < emails.size(); i++) {
                                    MailReceiver.EmailMessage emailMessage = emails.get(i);
                                    System.out.println("\n----- Email #" + (i + 1) + " -----");
                                    System.out.println("De: " + emailMessage.getFrom());
                                    System.out.println("Sujet: " + emailMessage.getSubject());
                                    System.out.println("Date: " + emailMessage.getReceivedDate());
                                    if (emailMessage.hasAttachments()) {
                                        System.out.println("[Contient des pièces jointes]");
                                    }
                                    if (emailMessage.isEncrypted()) {
                                        System.out.println("[Message chiffré]");
                                    }
                                    
                                    System.out.print("Afficher le contenu complet? (y/n): ");
                                    if (scanner.nextLine().toLowerCase().startsWith("y")) {
                                        System.out.println("\n--- CONTENU ---");
                                        System.out.println(emailMessage.getContent());
                                        System.out.println("---------------");
                                    }
                                }
                            }
                            
                            mailReceiver.disconnect();
                            
                        } catch (Exception e) {
                            Logger.error("Erreur lors de la récupération des emails: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    break;

                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
