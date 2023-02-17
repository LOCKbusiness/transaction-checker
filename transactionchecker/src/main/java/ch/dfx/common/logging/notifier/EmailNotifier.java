package ch.dfx.common.logging.notifier;

import java.util.Properties;

import javax.annotation.Nonnull;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.dfx.common.config.TransactionCheckerConfigEnum;
import ch.dfx.common.config.ConfigProvider;
import ch.dfx.common.errorhandling.DfxException;

/**
 * 
 */
public class EmailNotifier {
  private static final Logger LOGGER = LogManager.getLogger(EmailNotifier.class);

  private final Properties emailProperties;

  /**
   * 
   */
  public EmailNotifier() {
    this.emailProperties = new Properties();

    emailProperties.put("mail.smtp.auth", "true");
    emailProperties.put("mail.smtp.starttls.enable", "true");
    emailProperties.put("mail.smtp.ssl.protocols", "TLSv1.2");
    emailProperties.put("mail.smtp.host", "smtp.gmail.com");
    emailProperties.put("mail.smtp.port", "587");
  }

  /**
   *
   */
  public void sendMessage(@Nonnull String content) throws DfxException {
    LOGGER.trace("sendMessage()");

    try {
      String emailUser = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.EMAIL_USER);
      String emailPassword = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.EMAIL_PASSWORD);

      Session session = Session.getInstance(emailProperties, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(emailUser, emailPassword);
        }
      });

      // ...
      String emailFrom = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.EMAIL_FROM);
      String emailTo = ConfigProvider.getInstance().getValue(TransactionCheckerConfigEnum.EMAIL_TO);

      // ...
      Message message = new MimeMessage(session);

      message.setFrom(new InternetAddress(emailFrom));
      message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailTo));

      message.setSubject("[Transaction Check Server]: Test E-Mail");

      MimeBodyPart mimeBodyPart = new MimeBodyPart();
      mimeBodyPart.setContent(content, "text/html; charset=utf-8");

      Multipart multipart = new MimeMultipart();
      multipart.addBodyPart(mimeBodyPart);

      message.setContent(multipart);

      Transport.send(message);
    } catch (Exception e) {
      throw new DfxException("sendMessage", e);
    }
  }
}
