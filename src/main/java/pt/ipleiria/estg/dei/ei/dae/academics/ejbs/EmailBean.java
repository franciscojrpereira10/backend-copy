package pt.ipleiria.estg.dei.ei.dae.academics.ejbs;

import jakarta.ejb.Stateless;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Stateless
public class EmailBean {

    private static final Logger logger = Logger.getLogger(EmailBean.class.getName());

    public void send(String to, String subject, String text) {
        Thread thread = new Thread(() -> {
            try {
                Properties props = new Properties();
                // O docker define o host como "smtp" (nome do serviço)
                props.put("mail.smtp.host", "smtp");
                props.put("mail.smtp.port", "1025");
                props.put("mail.smtp.auth", "false"); // MailHog não requer auth
                props.put("mail.smtp.starttls.enable", "false");

                Session session = Session.getInstance(props, null);

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("noreply@academics.pt"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
                message.setSubject(subject);
                message.setText(text);

                Transport.send(message);
                logger.info("Email sent to " + to + " | Subject: " + subject);

            } catch (MessagingException e) {
                logger.log(Level.SEVERE, "Failed to send email to " + to, e);
            }
        });
        
        thread.start();
    }
}
