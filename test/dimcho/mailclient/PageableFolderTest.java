package dimcho.mailclient;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;

import dimcho.mailclient.PageableFolder;

public class PageableFolderTest {
	private static final String LOCALHOST = "127.0.0.1";
	private static final String USER_NAME = "dimcho";
	private static final String USER_EMAIL = "dimcho@localhost";
	private static final String USER_PASSWORD = "password";
	private static final String[] EMAIL_FROM = {"dimcho@localhost.com","noone@someone.com","someone@noone.com",
												"maxim@someone.com","eli@someone.com","test@test.com",
												"dimcho@localhost.com","noone@someone.com","someone@noone.com",
												"none@me.com", "some@none.com","D@dimcho.com",
												"maxim@someone.com","eli@someone.com","test@test.com"};
	
	private static final String[] EMAIL_SUBJECT = {"Test E-Mail","Hi, /n teting ...","Hello this is a test",
													"Testing,testing 1,2,3...","Tests, tests are the best","Today is not your lucky day",
													"Test E-Mail","Hi, /n teting ...","Hello this is a test",
													"Subject is not important","Some random subject", "Seriously who reads those",
													"Testing,testing 1,2,3...","Tests, tests are the best","Today is not your lucky day"};
	
	private static final String[] EMAIL_TEXT = {"This is a test e-mail.","Second test e-mail","Third test e-mail",
												"Everything is fine./nCall me ASAP./n{}@","Message text.Random words.Hello","No result",
												"This is a test e-mail.","Second test e-mail","Third test e-mail",
												"You just won a new iPhone 25S!\n PS: This is not a scam!","Some random text",
												"Again are you really reading thease.",
												"Everything is fine./nCall me ASAP./n{}@","Message text.Random words.Hello","No result"};
	
	private GreenMail mailServer;
	private GreenMailUser mockUser;
	Session session;
	private Folder folder;
	
	
	@Before
	public void setUp() throws Exception{
		// Initialize the mail server
		mailServer = new GreenMail(ServerSetupTest.POP3);
		mailServer.start();
		
		mockUser = mailServer.setUser(USER_EMAIL, USER_NAME, USER_PASSWORD);

		Properties props = new Properties();
		props.setProperty("mail.pop3.connectiontimeout", "5000");
		session = Session.getInstance(props);
		
		URLName urlName = new URLName("pop3", LOCALHOST, ServerSetupTest.POP3.getPort(),
				null, mockUser.getLogin(), mockUser.getPassword());
		
		Store store = session.getStore(urlName);
		store.connect();
		
		folder = store.getFolder("INBOX");
	}
	
	@After
	public void tearDown() throws MessagingException{
		folder.close(true);
		mailServer.stop();
	}
	
	private void setUpMessages(int messageCount) throws Exception{
		for (int i = 0; i < messageCount; i++) {
			MimeMessage message = new MimeMessage(session);
			message.setFrom(new InternetAddress(EMAIL_FROM[i]));
			message.setSubject(EMAIL_SUBJECT[i]);
			message.setText(EMAIL_TEXT[i]);
			message.setRecipient(Message.RecipientType.TO, new InternetAddress(USER_EMAIL));
			
			mockUser.deliver(message);
		} 
	}
	
	// Opens the folder with the specified access and returns a PageableFolder object
	private PageableFolder openInboxFolder(int access, int pageSize) throws Exception{
		folder.open(access);
		
		return new PageableFolder(folder,pageSize);
	}
	
	// Test the message data within the page
	private void testPage(Message[] currentPage, int pageNumber,int pageSize) throws Exception{
		int indexInPage = 0;
		int overAllIndex = pageNumber*pageSize;
		int messageCountWithinPage = currentPage.length + pageNumber*pageSize;
		
		for(int i = overAllIndex; i < messageCountWithinPage; i++){
			assertNotNull("Page Empty!",currentPage);
			assertEquals("Sender does not match!",EMAIL_FROM[i], currentPage[indexInPage].getFrom()[0].toString());
			assertEquals("Subject does not match!",EMAIL_SUBJECT[i], currentPage[indexInPage].getSubject());
			assertTrue("Message contents do not match!", String.valueOf(currentPage[indexInPage].getContent()).contains(EMAIL_TEXT[i]));
			
			indexInPage++;
		}
	}
	
	
	@Test
	// Tests a page which holds a single message
	// The page number and e-mail number are identical in this case
	public void oneMessageOnePageTest()throws Exception{
		setUpMessages(1);
		
		Message[] messages = openInboxFolder(Folder.READ_ONLY, 1).getNextPage();
		
		// Page specific tests
		testPage(messages,0,1);
	}
	
	
	@Test
	// Tests two pages which hold one mail each
	public void oneMessagePerTwoPagesTest() throws Exception{
		setUpMessages(2);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, 1);
		
		Message messages[];
		int pageNumber = 0;
		
		// The page number and e-mail number are identical in this case
		while(null != (messages = inboxFolder.getNextPage())){
			// case specific tests
			assertThat("The number of messages in the page do not match!",1,equalTo(1));
			
			// Page specific tests
			testPage(messages, pageNumber,1);			
			pageNumber++;
		}	
		
		assertEquals("The number of pages do not match!",2, pageNumber);
	}
	
	@Test
	// Test one full page and another partially filled one 
	public void oneFullPageOneNotTest() throws Exception{
		
		setUpMessages(3);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, 2);
		
		Message[] currentPage;
		
		// The page number and e-mail number do not match in this case
		int pageNumber = 0;
		while(null != (currentPage = inboxFolder.getNextPage())){
			// Case specific tests
			assertNotNull("The page is empty",currentPage);
			assertThat("The number of emails do not match",currentPage.length,lessThanOrEqualTo(2));
			assertThat("The number of emails do not match",currentPage.length,greaterThan(0));
			
			// Page specific tests
			testPage(currentPage, pageNumber, 2);
			
			pageNumber++;
		}
		// Case specific test
		assertEquals("The number of pages do not match!",2, pageNumber);
	}
	
	@Test
	// Tests a partially filled page with a large page size
	public void largePageNotFullTest() throws Exception{
		setUpMessages(6);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY,24);
		
		// There is only one page in this case
		Message[] currentPage = inboxFolder.getNextPage();
		
		// Case specific tests
		assertNotNull("The page is empty!",currentPage);
		assertEquals("The number of messages in the page does not match!",6, currentPage.length);
		
		// Page specific tests
		testPage(currentPage,0,24);
		
	}
	
	@Test
	// Tests a page with even number of e-mails in it
	public void evenNumberOfEmailsTest() throws Exception{
		setUpMessages(4);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, 7);
		
		// There is only one page in this case
		Message[] currentPage = inboxFolder.getNextPage();
				
		// Case specific tests
		assertNotNull("The page is empty!",currentPage);
		assertEquals("The number of messages in the page does not match!",4, currentPage.length);
		
		// Page specific tests
		testPage(currentPage, 0, 7);
		
	}
	
	@Test 
	// Tests a page with odd number of e-mails in it
	public void oddNumberOfEmailsTest() throws Exception{
		setUpMessages(5);		
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, 6);
		
		// There is only one page in this case
		Message[] currentPage = inboxFolder.getNextPage();
						
		// Case specific tests
		assertNotNull("The page is empty!",currentPage);
		assertEquals("The number of messages in the page does not match!",5, currentPage.length);
		
		// Page specific tests
		testPage(currentPage, 0, 6);
	}
	
	@Test 
	// Tests multiple pages filled with multiple e-mails
	public void multipleEmailsMultiplePagesTest() throws Exception{
		setUpMessages(15);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, 3);
		
		Message[] currentPage;
		int pageNumber = 0;
		
		while(null != (currentPage = inboxFolder.getNextPage())){
			// Case specific tests
			assertNotNull("The page is empty!",currentPage);
			assertEquals("The number of emails in the page do not match!",3,currentPage.length);
			
			// Page specific tests
			testPage(currentPage, pageNumber, 3);
			
			pageNumber++;
		}
		
		// Case specific test
		assertEquals("The number of pages do not match!",5, pageNumber);
	}
	
}
