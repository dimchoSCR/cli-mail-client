package dimcho.mailclient;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;

public class MailClient {
	
	public static void main(String[] args) {
		
		Properties properties = System.getProperties();
		//properties.setProperty("mail.store.protocol", "imaps");
		
		try {
			// set the properties to the session
			Session session = Session.getDefaultInstance(properties,null);
			
			// use pop3 protocol to store and retrieve messages
			Store store = session.getStore("pop3s");
			store.connect("pop3.abv.bg", "hacker_none@abv.bg","rycbardestroyar");
			
			Folder folder = store.getFolder("Inbox");
			folder.open(Folder.READ_ONLY);
			
			PageableFolder inboxFolder = new PageableFolder(folder);
			Message[] messages;
			while(null != (messages = inboxFolder.getNextPage())){
				listMails(messages);
			}
			
			/*
			System.out.println("Message sent from: " + messages[4].getFrom()[0]);
			System.out.println("Message subject: " + messages[4].getSubject());
			System.out.println("Message: ");
			
			InputStream mailIS = messages[4].getInputStream();
			BufferedReader reader = getReader(mailIS);
			
			String currentLine="";
			while((currentLine = reader.readLine()) != null){
				System.out.println(currentLine);
			}
			*/
					
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/*private static BufferedReader getReader(InputStream in){
		return new BufferedReader(new InputStreamReader(in));
	}
	*/
	
	private static void listMails(Message[] messages)throws Exception{
		for(Message currentMessage: messages){
			Address from = currentMessage.getFrom()[0];
			String fromName = from.toString();
			if(from instanceof InternetAddress){
				fromName = ((InternetAddress) from).toUnicodeString();
			}
			System.out.println("From: " + fromName + "\nSubject: " + currentMessage.getSubject());
			System.out.println("---------------------------------------------------------------------");
		}
	}

}
