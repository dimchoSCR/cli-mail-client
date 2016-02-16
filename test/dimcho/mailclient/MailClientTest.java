package dimcho.mailclient;

import static org.junit.Assert.*;

import java.io.File;
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
	private static final String MESSAGE_CONTENT_TEXT = "This is plain text.";
	private static final String MESSAGE_CONTENT_HTML = "<HTML><HEAD></HEAD>"
														+ "<BODY><P>This is plain text.</P>"
														+ "</BODY></HTML>";
	private static final String ATTACHMENT_FILE_PATH = "snapshot1.png";
	
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
	public void mimeMessageWithOnlyTextTest() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setText(MESSAGE_CONTENT_TEXT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY,1);
		String messageText = null;
		
		messageText = readMailText(inboxFolder.getNextPage()[0]).trim(); // Remove trailing white spaces after message

		assertNotNull("The message is empty!",messageText);
		assertThat("Message text does not match!",MESSAGE_CONTENT_TEXT,equalTo(messageText));
	}
	
	@Test
	public void mimeMessageWithTextAndHTMLTest() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		Multipart mPart = new MimeMultipart();
		
		BodyPart textPart = new MimeBodyPart();
		textPart.setText(MESSAGE_CONTENT_TEXT);
		
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(MESSAGE_CONTENT_HTML, "text/html");
		
		mPart.addBodyPart(textPart);
		mPart.addBodyPart(htmlPart);
		
		message.setContent(mPart);
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_WRITE,1);
		String messageText = null;
		messageText = readMailText(inboxFolder.getNextPage()[0]).trim();
		
		assertNotNull("Message is empty!", messageText);
		assertThat("Message content does not match!",MESSAGE_CONTENT_TEXT,equalTo(messageText));
	}
	
	@Test
	public void mimeMessageWithTextHtmlAndAttachments() throws Exception{
		MimeMessage message = new MimeMessage(session);
		message.setFrom(FROM);
		message.setSubject(MESSAGE_SUBJECT);
		message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
		
		Multipart mPart = new MimeMultipart();
		
		BodyPart textPart = new MimeBodyPart();
		textPart.setText(MESSAGE_CONTENT_TEXT);
		mPart.addBodyPart(textPart);
		
		BodyPart htmlPart = new MimeBodyPart();
		htmlPart.setContent(MESSAGE_CONTENT_HTML, "text/html");
		mPart.addBodyPart(htmlPart);
		
		MimeBodyPart attachment = new MimeBodyPart();
		attachment.attachFile(new File(ATTACHMENT_FILE_PATH), "image/png", null);
		mPart.addBodyPart(attachment);
		
		message.setContent(mPart);
		
		mockUser.deliver(message);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_WRITE,1);
		String messageText = null;
		messageText = readMailText(inboxFolder.getNextPage()[0]).trim();
		
		assertNotNull("Message is empty!", messageText);
		assertThat("Message content does not match!",MESSAGE_CONTENT_TEXT,equalTo(messageText));
	}
	
}