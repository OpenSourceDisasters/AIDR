package qa.qcri.aidr.common.util;

import java.io.UnsupportedEncodingException;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.log4j.Logger;

public class EmailClient {

	private static CommonConfigurator configProperties = CommonConfigurator.getInstance();
	private static Logger logger = Logger.getLogger(EmailClient.class.getName());

	public static void sendErrorMail(String subject, String body )
	{    
		// Recipient's email IDs.
		String recipients = configProperties.getProperty(CommonConfigurationProperty.RECIPIENT_EMAIL);
		//String[] recipients = toList.split(",");

		// Sender's details
		final String sender = configProperties.getProperty(CommonConfigurationProperty.SENDER_EMAIL);
		final String password = configProperties.getProperty(CommonConfigurationProperty.SENDER_PASS);

		//Set email server properties
		Properties props = new Properties();
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.smtp.host", configProperties.getProperty(CommonConfigurationProperty.SMTP_HOST));
		props.put("mail.smtp.port", configProperties.getProperty(CommonConfigurationProperty.SMTP_PORT));

		Session session = Session.getInstance(props,
				new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(sender, password);
			}
		});
		try{
			// Create a default MimeMessage object.
			MimeMessage message = new MimeMessage(session);
			
			// Set Subject: header field
			message.setSubject(configProperties.getProperty(CommonConfigurationProperty.AIDR_PROFILE) + subject);
			
			// Now set the actual message
			message.setText(body);

			// Set From: header field of the header.
			message.setFrom(new InternetAddress(sender,"AIDR Admin"));
			
			// Set To: header field of the header.
			message.addRecipients(Message.RecipientType.TO,InternetAddress.parse(recipients));
			// Send message
			Transport.send(message);
			logger.info("Sent message successfully to: " + recipients);
		} catch (AddressException e) {
			logger.error("Recipients address format exception"+e);
		}
		catch (MessagingException mex) {
			logger.error("Unable to send email to " + recipients + " due to " + mex.getCause());
			mex.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			logger.error("Unsupported encoding scheme used");
		}
	}
}
