package dimcho.mailclient;

import javax.mail.Folder;
import javax.mail.Message;

public class PageableFolder {
	private static final int DEFAULT_PAGE_SIZE = 30;
	
	private Folder folder;
	private int totalMessageCount;
	private int pageSize;
	private int totalPageCount;
	private int currentPage;
	
	
	public PageableFolder(Folder folder)throws Exception{
		this(folder,DEFAULT_PAGE_SIZE);
	}
	
	public PageableFolder(Folder folder, int pageSize) throws Exception{
		this.folder = folder;
		this.pageSize = pageSize;
		totalMessageCount = getTotalMessageCount();
		totalPageCount = getTotalPageCount();
		currentPage = 0;		
	}
	
	// returns the next page of e-mails
	// if there are no more pages the method returns null
	public Message[] getNextPage() throws Exception{

		if(currentPage == totalPageCount){
			return null;
		}
		
		int start = currentPage*pageSize + 1;
		int leftMessageCount = totalMessageCount - (start -1);
		
		currentPage++;
		
		// handles messages that do not completely fill a page
		if (leftMessageCount == 1) {
			Message[] messages = {folder.getMessage(start)};
			return messages;
		}else if(leftMessageCount < pageSize){
			return folder.getMessages(start,start + leftMessageCount -1);
		}
		
		// handles messages that fill the pageSize
		int end;
		if(totalMessageCount < pageSize){
			end = totalMessageCount;
		}else{
			end = start + pageSize -1;
		}
		
		return folder.getMessages(start,end);
	}
	
	private int getTotalPageCount() throws Exception{
		if(totalMessageCount < pageSize){
			return 1;
		}
		
		int totalPageCount = totalMessageCount / pageSize;
		return (0 == totalMessageCount % pageSize ) ? totalPageCount: totalPageCount + 1;
	}
	
	private int getTotalMessageCount() throws Exception{
		int totalMessageCount = folder.getMessageCount();
		
		// check if current folder is opened
		if (totalMessageCount == -1){
			throw new Exception(folder.getName() + " folder is not opened");
		}
		
		return totalMessageCount;
	}
}
