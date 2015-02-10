package info.hubbitus.imaptree.diff

import groovy.transform.TupleConstructor
import groovy.util.logging.Log4j2
import com.sun.mail.imap.AppendUID
import com.sun.mail.imap.IMAPMessage
import info.hubbitus.imaptree.diff.logdump.FolderMessagesDiffLoggerDefault
import info.hubbitus.imaptree.diff.logdump.FolderMessagesDiffLoggerFiles

import javax.mail.Message
import javax.mail.event.MessageChangedEvent
import javax.mail.event.MessageChangedListener
import javax.mail.event.MessageCountEvent
import javax.mail.event.MessageCountListener

/**
 *
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-31 01:04
 **/
@Log4j2
@TupleConstructor
@SuppressWarnings("ClashingTraitMethods") // Trait methods clashing desired because Trait chaining pattern used: http://groovy-lang.org/objectorientation.html#_chaining_behavior
class FolderMessagesCopier implements FolderMessagesDiffLoggerDefault, FolderMessagesDiffLoggerFiles{

	/**
	 * Due to the groovy BUG https://jira.codehaus.org/browse/GROOVY-7288 we can't it just @delegate like:
	 * @Delegate FolderMessagesDiff folders;
	 * and then use folder1 and folder2.
	 */
	FolderMessagesDiff folders;

	FolderMessagesCopier(FolderMessagesDiff folders) {
		this.folders = folders

		setupListeners();
	}

	protected void setupListeners() {
		// We provide only folder2 change
		folders.folder2messages.folder.addMessageChangedListener(
			[
				messageChanged: { MessageChangedEvent e ->
					log.info("messageChanged: e.getMessageChangeType(): ${e.getMessageChangeType()}; e.message: ${e.message}; UID: ${-1 == e.message.getUID() ? e.message.folder.getUID(e.message) : e.message.getUID()}; sha1: ${sha1(e.message.getMimeStream().text)}}");
					try{
						synchronized(this){
							diff_messagesAnomaliesHelper(
								[ ( e.message.sha1() ): (IMAPMessage)e.message ]
								,'messagesChanged'
								,folders.folder2messages
							);
						}
					}
					catch(Throwable t){
						log.error("Error happened process messagesChanged event: ", t);
					}
				}
			] as MessageChangedListener
		);

		folders.folder2messages.folder.addMessageCountListener(
			[
				messagesAdded   : { MessageCountEvent e ->
//					log.info("messagesAdded: ${e.messages.size()}; e.getType(): ${e.getType()}; e.isRemoved(): ${e.isRemoved()}; Message: ${messageToJson((IMAPMessage)e.messages[0])}");
					try{
						synchronized(this){
							diff_messagesAnomaliesHelper(
								e.messages.collectEntries{Message m->
									[ ( m.lastSha1 ): m ]
								}
								,'messagesAdded'
								,folders.folder2messages
							);
						}
					}
					catch(Throwable t){
						log.error("Error happened process messagesAdded event: ", t);
					}
				}
				,messagesRemoved: { MessageCountEvent e ->
//					log.info("messagesRemoved: ${e.messages.size()}; e.getType(): ${e.getType()}; e.isRemoved(): ${e.isRemoved()}; Messages: ${e.messages.collect{messageToJson((IMAPMessage)it)}}");
					try {
						synchronized(this){
							diff_messagesAnomaliesHelper(
								e.messages.collectEntries{Message m->
									[ ( m.lastSha1 ): m ]
								}
								,'messagesRemoved'
								,folders.folder2messages
							);
						}
					}
					catch(Throwable t){
						log.error("Error happened process messagesRemoved event: ", t);
					}
				}
			] as MessageCountListener
		);
	}

	/**
	 * Copy messages from folder 1 to folder 1 what not there by hashes
	 */
	@SuppressWarnings("GroovyAssignabilityCheck") // - Idea BUG: https://youtrack.jetbrains.com/issue/IDEA-135863
	AppendUID[] copyMissedMessagesToFolder2(){
		log.info("Copying missed mails from folder1 to folder2 (total ${folders.messagesInFolder1ButNotInFolder2*.value.size()}):");
		folders.folder2messages.reopenFolderReadWrite();
		// Base example from https://code.google.com/p/imapcopy/source/browse/trunk/ImapCopy/src/java/com/fisbein/joan/model/ImapCopier.java
		AppendUID[] appendedUids = folders.folder2messages.folder.appendUIDMessages((Message[])folders.messagesInFolder1ButNotInFolder2*.value)
		diff_appendedUIDs(appendedUids);
		folders.folder2messages.folder.expunge();
		appendedUids;
	}
}
