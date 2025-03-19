// package fr.insa.crypto.mail;

// import java.util.Scanner;

// import javax.mail.Message;
// import javax.mail.MessagingException;
// import javax.mail.Session;

// import fr.insa.crypto.utils.Logger;

// public class Test {
//     public static void main(String[] args) {
//         Logger.info("Starting email application...");

//         Scanner scanner = new Scanner(System.in);
//         Authentication auth = null;
//         Session session = null;
        
//         // Variables pour stocker les identifiants de façon simple
//         String userEmail = null;
//         String userPassword = null;

//         while (true) {
//             System.out.println("\n===== EMAIL APPLICATION =====");
//             System.out.println("1. Login");
//             System.out.println("2. Send Email");
//             System.out.println("3. Logout");
//             System.out.println("4. Exit");
//             System.out.println("5. Send Email with Attachment");
//             System.out.println("6. View Received Emails");
//             System.out.print("Enter your choice: ");

//             String choice = scanner.nextLine();

//             switch (choice) {
//                 case "1": // Login
//                     if (auth != null) {
//                         System.out.println("Already logged in. Please logout first.");
//                         break;
//                     }
//                     System.out.print("Enter your email: ");
//                     String email = scanner.nextLine();
//                     System.out.print("Enter your app key/password: ");
//                     String appKey = scanner.nextLine();
                    
//                     // Stocker les identifiants pour les réutiliser plus tard
//                     userEmail = email;
//                     userPassword = appKey;

//                     System.out.print("Use SSL instead of TLS? (y/n): ");
//                     boolean useSSL = scanner.nextLine().toLowerCase().startsWith("y");

//                     auth = new Authentication(email, appKey, useSSL);
//                     session = auth.getAuthenticatedSession();

//                     if (session != null) {
//                         Logger.info("Email authentication successful!");
//                     } else {
//                         Logger.error("Email authentication failed!");
//                         auth = null;
//                         userEmail = null;
//                         userPassword = null;
//                     }
//                     break;

//                 case "2": // Send Email
//                     if (auth == null || session == null) {
//                         Logger.error("You must login first!");
//                     } else {
//                         System.out.print("Enter recipient email: ");
//                         String toEmail = scanner.nextLine();

//                         System.out.print("Enter subject: ");
//                         String subject = scanner.nextLine();

//                         System.out.print("Enter email body: ");
//                         String body = scanner.nextLine();

//                         MailSender.sendEmail(session, toEmail, subject, body);
//                     }
//                     break;

//                 case "3": // Logout
//                     if (auth != null) {
//                         auth.logout();
//                         auth = null;
//                         session = null;
//                         // Effacer les identifiants lors de la déconnexion
//                         userEmail = null;
//                         userPassword = null;
//                         Logger.info("Logged out successfully.");
//                     } else {
//                         Logger.info("Not currently logged in.");
//                     }
//                     break;

//                 case "4": // Exit
//                     Logger.info("Exiting application...");
//                     scanner.close();
//                     System.exit(0);
//                     break;

//                 case "5": // Send Email with Attachment
//                     if (auth == null || session == null) {
//                         Logger.error("You must login first!");
//                     } else {
//                         try {
//                             System.out.print("Enter recipient email: ");
//                             String toEmail = scanner.nextLine();

//                             System.out.print("Enter subject: ");
//                             String subject = scanner.nextLine();

//                             System.out.print("Enter email body: ");
//                             String body = scanner.nextLine();
                            
//                             // Create attachment handler
//                             AttachmentHandler attachmentHandler = new AttachmentHandler();
                            
//                             boolean addMoreAttachments = true;
//                             while (addMoreAttachments) {
//                                 System.out.print("Enter file path to attach: ");
//                                 String filePath = scanner.nextLine();
                                
//                                 try {
//                                     attachmentHandler.addAttachment(filePath);
//                                     System.out.println("Attachment added successfully: " + filePath);
//                                 } catch (Exception e) {
//                                     Logger.error("Failed to add attachment: " + e.getMessage());
//                                 }
                                
//                                 System.out.print("Add another attachment? (y/n): ");
//                                 addMoreAttachments = scanner.nextLine().toLowerCase().startsWith("y");
//                             }
                            
//                             // Send email with attachments
//                             MailSender.sendEmailWithAttachments(session, toEmail, subject, body, attachmentHandler);
                            
//                         } catch (Exception e) {
//                             Logger.error("Error sending email with attachment: " + e.getMessage());
//                             e.printStackTrace();
//                         }
//                     }
//                     break;

//                 case "6": // View Received Emails
//                     if (auth == null || session == null || userEmail == null || userPassword == null) {
//                         Logger.error("You must login first!");
//                     } else {
//                         try {
//                             System.out.println("\n===== VIEWING RECEIVED EMAILS =====");
//                             System.out.print("Enter mail server (e.g., pop.gmail.com): ");
//                             String host = scanner.nextLine();
                            
//                             // Si aucun serveur n'est indiqué, utiliser gmail par défaut
//                             if (host == null || host.trim().isEmpty()) {
//                                 host = "pop.gmail.com";
//                                 System.out.println("Using default server: " + host);
//                             }
                            
//                             // Créer un MailReceiver
//                             MailReceiver mailReceiver = new MailReceiver(host, "995");
                            
//                             try {
//                                 // Utiliser directement les identifiants stockés
//                                 mailReceiver.connect(userEmail, userPassword);
//                                 mailReceiver.openFolder("INBOX", true);
                                
//                                 Message[] messages = mailReceiver.getMessages();
                                
//                                 if (messages.length == 0) {
//                                     System.out.println("Aucun message trouvé.");
//                                 } else {
//                                     System.out.println("\n===== MAILS REÇUS =====");
//                                     System.out.println("Nombre de messages: " + messages.length);
                                    
//                                     for (int i = 0; i < messages.length; i++) {
//                                         Message message = messages[i];
//                                         System.out.println("\n----- Email #" + (i + 1) + " -----");
//                                         System.out.println("De: " + (message.getFrom().length > 0 ? message.getFrom()[0] : "Inconnu"));
//                                         System.out.println("Sujet: " + message.getSubject());
//                                         System.out.println("Date: " + message.getReceivedDate());
                                        
//                                         System.out.print("Afficher le contenu complet? (y/n): ");
//                                         if (scanner.nextLine().toLowerCase().startsWith("y")) {
//                                             System.out.println("\n--- CONTENU ---");
//                                             System.out.println(message.getContent().toString());
//                                             System.out.println("---------------");
//                                         }
//                                     }
//                                 }
                                
//                                 mailReceiver.close();
                                
//                             } catch (MessagingException e) {
//                                 Logger.error("Erreur lors de la connexion au serveur: " + e.getMessage());
//                                 System.out.println("Voulez-vous réessayer avec d'autres identifiants? (y/n): ");
//                                 if (scanner.nextLine().toLowerCase().startsWith("y")) {
//                                     System.out.print("Enter your email: ");
//                                     userEmail = scanner.nextLine();
//                                     System.out.print("Enter your password: ");
//                                     userPassword = scanner.nextLine();
                                    
//                                     try {
//                                         mailReceiver.connect(userEmail, userPassword);
//                                         mailReceiver.openFolder("INBOX", true);
                                        
//                                         Message[] messages = mailReceiver.getMessages();
                                        
//                                         // Répéter le code d'affichage des messages...
//                                         if (messages.length == 0) {
//                                             System.out.println("Aucun message trouvé.");
//                                         } else {
//                                             System.out.println("\n===== MAILS REÇUS =====");
//                                             System.out.println("Nombre de messages: " + messages.length);
                                            
//                                             for (int i = 0; i < messages.length; i++) {
//                                                 Message message = messages[i];
//                                                 System.out.println("\n----- Email #" + (i + 1) + " -----");
//                                                 System.out.println("De: " + (message.getFrom().length > 0 ? message.getFrom()[0] : "Inconnu"));
//                                                 System.out.println("Sujet: " + message.getSubject());
//                                                 System.out.println("Date: " + message.getReceivedDate());
                                                
//                                                 System.out.print("Afficher le contenu complet? (y/n): ");
//                                                 if (scanner.nextLine().toLowerCase().startsWith("y")) {
//                                                     System.out.println("\n--- CONTENU ---");
//                                                     System.out.println(message.getContent().toString());
//                                                     System.out.println("---------------");
//                                                 }
//                                             }
//                                         }
                                        
//                                     } catch (MessagingException ex) {
//                                         Logger.error("Échec de la connexion avec les nouveaux identifiants: " + ex.getMessage());
//                                     }
//                                 }
//                             }
//                         } catch (Exception e) {
//                             Logger.error("Erreur lors de la récupération des emails: " + e.getMessage());
//                             e.printStackTrace();
//                         }
//                     }
//                     break;

//                 default:
//                     System.out.println("Invalid choice. Please try again.");
//             }
//         }
//     }
// }
