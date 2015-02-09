package info.hubbitus.imaptree.diff

import groovy.util.logging.Log4j2
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPMessage
import info.hubbitus.imaptree.diff.logdump.FolderMessagesDiffLoggerDefault
import info.hubbitus.imaptree.diff.logdump.FolderMessagesDiffLoggerFiles
import info.hubbitus.imaptree.utils.cache.MessagesCache

import javax.mail.Folder
import javax.mail.Message

/**
 * Class represent and handle two IMAPFolder differences.
 * Please note - most structures evaluated lazily and once. So, it does not handle folders content change! Instead
 * designed to store snapshot of it.
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-30 22:38
 **/
@Log4j2
@SuppressWarnings("ClashingTraitMethods") // Trait methods clashing desired because Trait chaining pattern used: http://groovy-lang.org/objectorientation.html#_chaining_behavior
class FolderMessagesDiff implements FolderMessagesDiffLoggerDefault, FolderMessagesDiffLoggerFiles{
	final MessagesCache folder1messages;
	final MessagesCache folder2messages;

	FolderMessagesDiff(IMAPFolder folder1, IMAPFolder folder2) {
		this.folder1messages = new MessagesCache(folder1)
		this.folder2messages = new MessagesCache(folder2)

		diff_init();
	}

	@Lazy
	Map<String,IMAPMessage> messagesInFolder1ButNotInFolder2 = {
		folder1messages.hashes.findAll { it.key in (folder1messages.hashes*.key - folder2messages.hashes*.key) }
	}()

	@Lazy
	Map<String,IMAPMessage> messagesInFolder2ButNotInFolder1 = {
		folder2messages.hashes.findAll{ it.key in (folder2messages.hashes*.key - folder1messages.hashes*.key) }
	}()

	@Lazy
	Map<String,List<IMAPMessage>> folder1MessagesWithNonUniqueHashes = {
		(folder1.messages as List<IMAPMessage>).groupBy{ it.sha1() }.findAll{ it.value.size() > 1 }
	}()

	@Lazy
	Map<String,List<IMAPMessage>> folder2MessagesWithNonUniqueHashes = {
		(folder2.messages as List<IMAPMessage>).groupBy{ it.sha1() }.findAll{ it.value.size() > 1 }
	}()

	void dumpAnomalies(){
		if (folder1MessagesWithNonUniqueHashes || folder2MessagesWithNonUniqueHashes || messagesInFolder1ButNotInFolder2 || messagesInFolder2ButNotInFolder1){
			['folder1MessagesWithNonUniqueHashes', 'folder2MessagesWithNonUniqueHashes'].each{
				diff_folderAnomaliesHelper(this."$it", it);
			}

			['messagesInFolder1ButNotInFolder2', 'messagesInFolder2ButNotInFolder1'].each{
				diff_messagesAnomaliesHelper(this."$it", it);
			}
		}
		else{
			log.debug('No anomalies found (folders messages content are equal)');
		}
	}

	@Override
	String toString() {
		diffMetricsCount.toString()
	}

	@Lazy Map diffMetricsCount = {
		[
			'Messages in 1st folder': folder1.messages.size()
			,'Messages unique by hash in 1st folder': folder1.messages*.sha1().unique().size()
			,'Messages in 2nd folder': folder2.messages.size()
			,'Messages unique by hash in 2nd folder': folder2.messages*.sha1().unique().size()
			// Very good example about map diff - http://groovyconsole.appspot.com/script/364002
			,'In Folder1 but NOT in Folder2': messagesInFolder1ButNotInFolder2.size()
			,'In Folder2 but NOT in Folder1': messagesInFolder2ButNotInFolder1.size()
		]
	}()

	void dump(String header = ''){
		if (header) log.debug(header);
		diff_metricsCount(diffMetricsCount);
		dumpAnomalies();
	}
}