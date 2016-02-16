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
	
	private static final int PAGE_SIZE = 1;
	private static final int MESSAGE_COUNT_ONE_MESSAGE_PER_TWO_PAGES = 2;
	private static final int PAGE_SIZE_ONE_FULL_PAGE_ONE_NOT = 2;
	private static final int MESSAGE_COUNT_ONE_FULL_PAGE_ONE_NOT = 3;
	private static final int MESSAGE_COUNT_LARGE_PAGE_NOT_FULL= 6;
	private static final int PAGE_SIZE_LARGE_PAGE_NOT_FULL = 24;
	private static final int MESSAGE_COUNT_EVEN_NUMBER_OF_EMAILS = 4;
	private static final int PAGE_SIZE_EVEN_NUMBER_OF_EMAILS = 7;
	private static final int MESSAGE_COUNT_ODD_NUMBER_OF_EMAILS = 5;
	private static final int PAGE_SIZE_ODD_NUMBER_OF_EMAILS = 6;
	private static final int MESSAGE_COUNT_MULTIPLE_EMAILS_MULTIPLE_PAGES = 15;
	private static final int PAGE_SIZE_MULTIPLE_EMAILS_MULTIPLE_PAGES = 3;
	
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
	
	private void sendMessagesToServer(int messageCount) throws Exception{
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
	
	private void performPageSpecificTests(Message[] currentPage, int pageNumber,int pageSize) throws Exception{
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
	public void oneMessageOnePageTest()throws Exception{
		// The page consists of one message
		// The page number and e-mail number are identical in this case
		sendMessagesToServer(1);
		
		Message[] messages = openInboxFolder(Folder.READ_ONLY, PAGE_SIZE).getNextPage();
		
		performPageSpecificTests(messages,0,PAGE_SIZE);
	}
	
	
	@Test
	public void oneMessagePerTwoPagesTest() throws Exception{
		// Each page consists of only one message
		sendMessagesToServer(MESSAGE_COUNT_ONE_MESSAGE_PER_TWO_PAGES);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, PAGE_SIZE);
		
		Message messages[];
		int pageNumber = 0;
		
		// The page number and e-mail number are identical in this case
		while(null != (messages = inboxFolder.getNextPage())){
			// case specific tests
			assertThat("The number of messages in the page do not match!",1,equalTo(PAGE_SIZE));
			
			performPageSpecificTests(messages, pageNumber,PAGE_SIZE);			
			pageNumber++;
		}	
		
		assertEquals("The number of pages do not match!",2, pageNumber);
	}
	
	@Test
	public void oneFullPageOneNotTest() throws Exception{
		
		sendMessagesToServer(MESSAGE_COUNT_ONE_FULL_PAGE_ONE_NOT);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, PAGE_SIZE_ONE_FULL_PAGE_ONE_NOT);
		
		Message[] currentPage;
		
		// The page number and e-mail number do not match in this case
		int pageNumber = 0;
		while(null != (currentPage = inboxFolder.getNextPage())){
			// case specific tests
			assertNotNull(currentPage);
			assertThat(currentPage.length,lessThanOrEqualTo(PAGE_SIZE_ONE_FULL_PAGE_ONE_NOT));
			assertThat(currentPage.length,greaterThan(0));
			
			performPageSpecificTests(currentPage, pageNumber, PAGE_SIZE_ONE_FULL_PAGE_ONE_NOT);
			
			pageNumber++;
		}
		
		assertEquals("The number of pages do not match!",2, pageNumber);
	}
	
	@Test
	public void largePageNotFullTest() throws Exception{
		sendMessagesToServer(MESSAGE_COUNT_LARGE_PAGE_NOT_FULL);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY,PAGE_SIZE_LARGE_PAGE_NOT_FULL);
		
		// There is only one page in this case
		Message[] currentPage = inboxFolder.getNextPage();
		
		// case specific tests
		assertNotNull("The page is empty!",currentPage);
		assertEquals("The number of messages in the page does not match!",MESSAGE_COUNT_LARGE_PAGE_NOT_FULL, currentPage.length);
		
		performPageSpecificTests(currentPage,0,PAGE_SIZE_LARGE_PAGE_NOT_FULL);
		
	}
	
	@Test
	public void evenNumberOfEmailsTest() throws Exception{
		sendMessagesToServer(MESSAGE_COUNT_EVEN_NUMBER_OF_EMAILS);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, PAGE_SIZE_EVEN_NUMBER_OF_EMAILS);
		
		// There is only one page in this case
		Message[] currentPage = inboxFolder.getNextPage();
				
		// case specific tests
		assertNotNull("The page is empty!",currentPage);
		assertEquals("The number of messages in the page does not match!",MESSAGE_COUNT_EVEN_NUMBER_OF_EMAILS, currentPage.length);
		
		performPageSpecificTests(currentPage, 0, PAGE_SIZE_EVEN_NUMBER_OF_EMAILS);
		
	}
	
	@Test 
	public void oddNumberOfEmailsTest() throws Exception{
		sendMessagesToServer(MESSAGE_COUNT_ODD_NUMBER_OF_EMAILS);		
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, PAGE_SIZE_ODD_NUMBER_OF_EMAILS);
		
		// There is only one page in this case
		Message[] currentPage = inboxFolder.getNextPage();
						
		// case specific tests
		assertNotNull("The page is empty!",currentPage);
		assertEquals("The number of messages in the page does not match!",MESSAGE_COUNT_ODD_NUMBER_OF_EMAILS, currentPage.length);
		
		performPageSpecificTests(currentPage, 0, PAGE_SIZE_ODD_NUMBER_OF_EMAILS);
	}
	
	@Test 
	public void multipleEmailsMultiplePagesTest() throws Exception{
		sendMessagesToServer(MESSAGE_COUNT_MULTIPLE_EMAILS_MULTIPLE_PAGES);
		
		PageableFolder inboxFolder = openInboxFolder(Folder.READ_ONLY, PAGE_SIZE_MULTIPLE_EMAILS_MULTIPLE_PAGES);
		
		Message[] currentPage;
		int pageNumber = 0;
		
		while(null != (currentPage = inboxFolder.getNextPage())){
			assertNotNull("The page is empty!",currentPage);
			assertEquals("The number of emails in the page do not match!",PAGE_SIZE_MULTIPLE_EMAILS_MULTIPLE_PAGES,currentPage.length);
			
			performPageSpecificTests(currentPage, pageNumber, PAGE_SIZE_MULTIPLE_EMAILS_MULTIPLE_PAGES);
			pageNumber++;
		}
		
		assertEquals("The number of pages do not match!",5, pageNumber);
	}
	
}
