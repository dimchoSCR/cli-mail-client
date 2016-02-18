package dimcho.mailclient;

import static org.junit.Assert.*;

import java.util.Properties;

import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.After;
import org.junit.Before;

import static org.hamcrest.Matchers.*;

import org.junit.Test;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import static dimcho.mailclient.MailClient.readMailText;

public class MailClientTest {
	
	private static final String USER_EMAIL = "dimcho@localhost";
	private static final String USERNAME = "dimcho";
	private static final String USER_PASSWORD = "password";
	private static final String LOCALHOST = "127.0.0.1";
	
	private static final String FROM = "none@dimcho.com";
	private static final String MESSAGE_SUBJECT = "Test e-mail";
	
	private GreenMail mailServer;
	private GreenMailUser mockUser;
	private Session session;
	private Folder folder;
	
	@Before
	public void setUp() throws Exception{
		mailServer = new GreenMail(ServerSetupTest.POP3);
		mailServer.start();
		
		mockUser = mailServer.setUser(USER_EMAIL, USERNAME, USER_PASSWORD);
		
		// connect to the mail server with the specified properties and protocol
		Properties props = new Properties();
		props.setProperty("mail.pop3.connectiontimeout", "5000");
		
		session = Session.getInstance(props);
		URLName urlName = new URLName("pop3",LOCALHOST, ServerSetupTest.POP3.getPort(),
				null, mockUser.getLogin(), mockUser.getPassword());
		
		Store store = session.getStore(urlName);
		store.connect();
		
		folder = store.getFolder("INBOX");
	}
	
	@After
	public void tearDown() throws Exception {
		folder.close(true);
		mailServer.stop();
	}
	
	private PageableFolder openInboxFolder(int access, int pageSize) throws Exception{
		folder.open(access);
		return new PageableFolder(folder,pageSize);
	}
	
	@Test
	// Tests reading the plain text from a multipart message containing only plain text
	public void mimeMessageWithOnlyTextTest() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setText("Test e-mail");
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY,1);
		String messageText = null;
		
		messageText = readMailText(inboxFolder.getNextPage()[0]).trim(); // Remove trailing white spaces after message

		assertNotNull("The message is empty!",messageText);
		assertThat("Message text does not match!","Test e-mail",equalTo(messageText));
	}
	
	@Test
	// Tests reading the plain text from a multipart message containing plain text and html
	public void mimeMessageWithTextAndHTMLTest() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		Multipart mPart = new MimeMultipart();
		
		BodyPart textPart = new MimeBodyPart();
		textPart.setText("Test e-mail");
		
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent("<b>Test e-mail</b>", "text/html");
		
		mPart.addBodyPart(textPart);
		mPart.addBodyPart(htmlPart);
		
		message.setContent(mPart);
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_WRITE,1);
		String messageText = null;
		messageText = readMailText(inboxFolder.getNextPage()[0]).trim();
		
		assertNotNull("Message is empty!", messageText);
		assertThat("Message content does not match!","Test e-mail",equalTo(messageText));
	}
	
	@Test
	// Tests reading the plain text from a multipart message containing plain text, html and an attachment
	public void mimeMessageWithTextHtmlAndAttachments() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		Multipart mPart = new MimeMultipart();
		
		BodyPart textPart = new MimeBodyPart();
		textPart.setText("Test e-mail");
		mPart.addBodyPart(textPart);
		
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent("<b>Test e-mail</b>", "text/html");
		mPart.addBodyPart(htmlPart);
		
		MimeBodyPart attachment = new MimeBodyPart();
		attachment.attachFile("snapshot1.png", "image/png", null);
		mPart.addBodyPart(attachment);
		
		message.setContent(mPart);
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_WRITE,1);
		String messageText = null;
		messageText = readMailText(inboxFolder.getNextPage()[0]).trim();
		
		assertNotNull("Message is empty!", messageText);
		assertThat("Message content does not match!","Test e-mail",equalTo(messageText));
	}

	@Test
	// Tests reading the plain text from a multipart message containing ONLY html
	public void mimeMessageWithOnlyHtml() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		Multipart mPart = new MimeMultipart();
		
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent("<b>Test e-mail</b>", "text/html");
		
		mPart.addBodyPart(htmlPart);
		message.setContent(mPart);
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_WRITE,1);
		String messageText;
		
		// The method should return null because there is no plain text in the message
		messageText = readMailText(inboxFolder.getNextPage()[0]);
		
		assertNull("Message is not empty but it should be!",messageText);
	}
	
	@Test
	// Tests reading the plain text from a multipart message containing ONLY an attachment
	public void mimeMessageWithOnlyAnAttachment() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		Multipart mPart = new MimeMultipart();
		
		MimeBodyPart attachment = new MimeBodyPart();
		attachment.attachFile("snapshot1.png", "image/png", null);
		mPart.addBodyPart(attachment);
		
		message.setContent(mPart);
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_WRITE,1);
		String messageText;
		
		// The method should return null because there is no plain text in the message
		messageText = readMailText(inboxFolder.getNextPage()[0]);
		
		assertNull("Message is not empty but it should be!",messageText);
	}
	
	@Test
	// Tests reading the plain text from a multipart message containing BOTH html and an attachment
	public void mimeMessageWithHtmlAndAttachment() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		Multipart mPart = new MimeMultipart();
		
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent("<b>Test e-mail</b>", "text/html");
		mPart.addBodyPart(htmlPart);
		
		MimeBodyPart attachment = new MimeBodyPart();
		attachment.attachFile("snapshot1.png", "image/png", null);
		mPart.addBodyPart(attachment);
		
		message.setContent(mPart);
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_WRITE,1);
		String messageText;
		
		// The method should return null because there is no plain text in the message
		messageText = readMailText(inboxFolder.getNextPage()[0]);
		
		assertNull("Message is not empty but it should be!",messageText);
	}
}
