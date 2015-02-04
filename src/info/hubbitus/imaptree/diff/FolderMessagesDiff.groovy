package info.hubbitus.imaptree.diff

import groovy.util.logging.Log4j2
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPMessage
import groovy.json.JsonOutput
import info.hubbitus.imaptree.diff.logdump.FolderMessagesDiffLoggerDefault
import info.hubbitus.imaptree.diff.logdump.FolderMessagesDiffLoggerFiles

import javax.mail.Folder
import javax.mail.Message
import java.lang.reflect.Field
import java.security.MessageDigest

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
	IMAPFolder folder1;
	IMAPFolder folder2;

	FolderMessagesDiff(IMAPFolder folder1, IMAPFolder folder2) {
		this.folder1 = folder1
		this.folder2 = folder2

		if(!folder1.open)
			folder1.open(Folder.READ_ONLY)
		if(!folder2.open)
			folder2.open(Folder.READ_ONLY)
	}

	/**
	 * Create new object to calculate new state after some changes, possibly to compare it to previous
	 *
	 * @param previous
	 */
	FolderMessagesDiff(FolderMessagesDiff previous){
		this(previous.folder1, previous.folder2);
	}

	/**
	 * Implementation from http://groovyconsole.appspot.com/script/642001
	 */
	static String sha1(String str) {
		return new BigInteger(1, MessageDigest.getInstance('SHA1').digest(str.getBytes())).toString(16);
	}

	static {
		// Last save for deleted messages
		IMAPMessage.metaClass.lastSha1 = '';
		IMAPMessage.metaClass.sha1 = {
			delegate.lastSha1 = sha1(delegate.getMimeStream().text + delegate.getReceivedDate());
		}
	}

	@Lazy
	Map<String,Map<String,IMAPMessage>> hashes = [
		folder1: folder1.messages.collectEntries{Message m->
			 [ ( m.sha1() ): m ]
		}
		,folder2: folder2.messages.collectEntries{Message m->
			[ ( m.sha1() ): m ]
		}
	];

	@Lazy
	Map<String,IMAPMessage> messagesInFolder1ButNotInFolder2 = {
		hashes.folder1.findAll { it.key in (hashes.folder1*.key - hashes.folder2*.key) }
	}()

	@Lazy
	Map<String,IMAPMessage> messagesInFolder2ButNotInFolder1 = {
		hashes.folder2.findAll { it.key in (hashes.folder2*.key - hashes.folder1*.key) }
	}()

	@Lazy
	Map<String,List<IMAPMessage>> folder1MessagesWithNonUniqueHashes = {
		(Map<String,List<IMAPMessage>>)folder1.messages.groupBy{ it.sha1() }.findAll{ it.value.size() > 1 }
	}()

	@Lazy
	Map<String,List<IMAPMessage>> folder2MessagesWithNonUniqueHashes = {
		(Map<String,List<IMAPMessage>>)folder2.messages.groupBy{ it.sha1() }.findAll{ it.value.size() > 1 }
	}()

	@Override
	String toString() {
		[
			'Messages in 1st folder': folder1.messages.size()
			,'Messages unique by hash in 1st folder': folder1.messages*.sha1().unique().size()
			,'Messages in 2nd folder': folder2.messages.size()
			,'Messages unique by hash in 2nd folder': folder2.messages*.sha1().unique().size()
			// Very good example about map diff - http://groovyconsole.appspot.com/script/364002
			,'In Folder1 but NOT in Folder2': messagesInFolder1ButNotInFolder2.size()
			,'In Folder2 but NOT in Folder1': messagesInFolder2ButNotInFolder1.size()
			,'In folder1 dupes by hashes': folder1MessagesWithNonUniqueHashes.collectEntries{ [(it.key): it.value.size()] }
			,'In folder2 dupes by hashes': folder2MessagesWithNonUniqueHashes.collectEntries{ [(it.key): it.value.size()] }
		].toString()
	}

	@SuppressWarnings("GroovyAccessibility") // there many hacks to access private fields for deleted messages.
	static String messageToJson(IMAPMessage m, List<String> headers = [], boolean prettyPrint = false){
//???	assert m.getSize() == m.getMimeStream().text.size()
		String res;
		if(m.expunged){ // Only base info and prefetched early data
			res = JsonOutput.toJson(
				[
					deleted: true
					,userFlags: m.@flags.userFlags // Direct field access without try refresh from server what lead to javax.mail.MessageRemovedException
					,UID: (-1 == m.getUID() ? ((IMAPFolder)m.folder).getUID(m) : m.getUID())
					,getReceivedDate: { // Access private field instead of MessageRemovedException (can't use just m.@ notation because it java class) (http://stackoverflow.com/questions/1196192/how-do-i-read-a-private-field-in-java)
						Field f = IMAPMessage.getDeclaredField('receivedDate')
						f.setAccessible(true);
						f.get(m)
					}
//					,getMessageNumber: m.getMessageNumber()
//					,size: { // Access private field instead of MessageRemovedException (can't use just m.@ notation because it java class)
//						Field f = IMAPMessage.getDeclaredField('size')
//						f.setAccessible(true);
//						f.get(m)
//					}
					,sha1: m.lastSha1
				]
			);
		}
		else{
			res = JsonOutput.toJson(
				[
					userFlags: m.getFlags().getUserFlags()
					,UID: (-1 == m.getUID() ? ((IMAPFolder)m.folder).getUID(m) : m.getUID())
//					,getMessageID: m.getMessageID()
//					,getModSeq: m.getModSeq()
					,getReceivedDate: m.getReceivedDate()
//					,getMessageNumber: m.getMessageNumber()
//					,getSequenceNumber: m.getSequenceNumber()
//					,size: m.getSize()
					,sha1: sha1(m.getMimeStream().text)
				]
				+
				m.getMatchingHeaders((String[])headers).toList().collectEntries{
					["Header <${it.name}>": it.value ]
				}
			);
		}
		return (prettyPrint ? JsonOutput.prettyPrint(res) : res);
	}

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

	void dump(String header = ''){
		if (header) log.debug(header);
		log.debug(toString());
		dumpAnomalies();
	}
}