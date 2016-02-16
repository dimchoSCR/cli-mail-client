package dimcho.mailclient;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
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
				
				/*for(int i = 0;i<messages.length;i++){
					System.out.println("Message text: ");
					String text;
					if(null == (text = readMailText(messages[i]))){
						System.out.println("No text available in this message! Please view it in your browser.");
					}else{
						System.out.println(text);
					}
				}*/
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	
	/*private static BufferedReader getReader(InputStream in){
		return new BufferedReader(new InputStreamReader(in));
	}*/
	
	
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
	
	public static String readMailText(Part part) throws Exception {
		
		// returns the plain text
		if(part.isMimeType("text/plain")){
			String text = (String) part.getContent();
			return text;
		}
		
		// Gets the plain/text part from a multipart/alternative subtype 
		Multipart mPart;
		if(part.isMimeType("multipart/alternative")){
			mPart = (Multipart) part.getContent();
				
			for(int i = 0;i<mPart.getCount();i++){
				Part bodyPart = mPart.getBodyPart(i);
				
				if(bodyPart.isMimeType("text/plain")){
					return readMailText(bodyPart);
				}
			}
			
		// Gets the plain/text part of the multipart/* subtype
		}else if(part.isMimeType("multipart/*")){
			mPart = (Multipart) part.getContent();
			for(int i = 0;i < mPart.getCount();i++){
				Part bodyPart = mPart.getBodyPart(i);
				
				String text = readMailText(bodyPart);
				if(null != text){
					return text;
				}
			}
		}
		
		//  The function returns null if the message contains no readable text
		return null;
		
		/*InputStream mailIS = message.getInputStream();
		 
		System.out.println("From: " + message.getFrom()[0].toString());
		System.out.println("Subject: " + message.getSubject());
		System.out.println("---------------------------------------------------------------------");
		 
		BufferedReader reader = getReader(mailIS);
		 
		String currentLine="";
		while(null != (currentLine = reader.readLine())){
			System.out.println(currentLine);
		}
		 
		System.out.println("---------------------------------------------------------------------");
		*/
	}

}
