package info.hubbitus.imaptree.diff

import groovy.util.logging.Log4j2
import com.sun.mail.imap.AppendUID
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPMessage

import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.UIDFolder
import javax.mail.event.MessageChangedEvent
import javax.mail.event.MessageChangedListener
import javax.mail.event.MessageCountEvent
import javax.mail.event.MessageCountListener

import static info.hubbitus.imaptree.diff.FolderMessagesDiff.messageToJson

/**
 *
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-31 01:04
 **/
@Log4j2
//@TupleConstructor
class FolderMessagesCopier {
	@Delegate FolderMessagesDiff folders;

	static{
		AppendUID.metaClass.asString = {
			return "{uid:${delegate.uid},uidvalidity:${delegate.uidvalidity}}"
		}
	}

	FolderMessagesCopier(FolderMessagesDiff folders) {
		this.folders = folders

		setupListeners();
		reopenFoldersReadWrite();
	}

	protected void setupListeners() {
		// We provide only folder2 change
		folder2.addMessageChangedListener(
			[
				messageChanged: { MessageChangedEvent e ->
					log.info("e.getMessageChangeType(): ${e.getMessageChangeType()}; e.message: ${e.message}; UID: ${-1 == e.mesage.getUID() ? e.message.folder.getUID(e.message) : e.message.getUID()}; sha1: ${sha1(e.message.getMimeStream().text)}}");
				}
			] as MessageChangedListener
		);

		folder2.addMessageCountListener(
			[
				messagesAdded   : { MessageCountEvent e ->
					log.info("messagesAdded: ${e.messages.size()}; e.getType(): ${e.getType()}; e.isRemoved(): ${e.isRemoved()}; Message: ${messageToJson((IMAPMessage)e.messages[0])}");
				}
				,messagesRemoved: { MessageCountEvent e ->
					log.info("messagesRemoved: ${e.messages.size()}; e.getType(): ${e.getType()}; e.isRemoved(): ${e.isRemoved()}; Messages: ${e.messages.collect{messageToJson((IMAPMessage)e.messages[0])}}");
				}
			] as MessageCountListener
		);
	}

	void reopenFoldersReadWrite(){
		[folder1, folder2].each{IMAPFolder folder->
			if (Folder.READ_ONLY == folder.getMode()){
				folder.close(false);
			}
			if (!folder.open){
				folder.open(Folder.READ_WRITE);
			}
			FetchProfile fp = new FetchProfile();
			// Prefetch all possible to speedup operations on content
			fp.add(FetchProfile.Item.CONTENT_INFO);
			fp.add(FetchProfile.Item.ENVELOPE);
			fp.add(FetchProfile.Item.FLAGS);
			fp.add(FetchProfile.Item.SIZE);
			fp.add(UIDFolder.FetchProfileItem.UID);
			folder.fetch(folder.messages, fp);
		}
	}

	/**
	 * Copy messages from folder 1 to folder 1 what not there by hashes
	 */
	@SuppressWarnings("GroovyAssignabilityCheck") // - Idea BUG: https://youtrack.jetbrains.com/issue/IDEA-135863
	AppendUID[] copyMissedMessagesToFolder2(){
		Map<String,Expando> map = [:];
		map*.value.size();
		log.info("Copying missed mails from folder1 to folder2 (total ${messagesInFolder1ButNotInFolder2*.value.size()}):");
		// Base example from https://code.google.com/p/imapcopy/source/browse/trunk/ImapCopy/src/java/com/fisbein/joan/model/ImapCopier.java
		AppendUID[] appendRes = folder2.appendUIDMessages((Message[])messagesInFolder1ButNotInFolder2*.value);
		folder2.expunge();
		folder1.expunge();
		log.debug('Appended UIDs: ' + appendRes.collect{ it.asString() });
		return appendRes;
	}
}
