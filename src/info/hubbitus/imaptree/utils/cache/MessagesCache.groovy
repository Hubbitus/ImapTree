package info.hubbitus.imaptree.utils.cache

import groovy.util.logging.Log4j2
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPMessage
import groovy.json.JsonOutput

import javax.mail.FetchProfile
import javax.mail.Folder
import javax.mail.Message
import javax.mail.MessageRemovedException
import javax.mail.UIDFolder
import java.lang.reflect.Field
import java.security.MessageDigest

/**
 * Class to handle full folder prefetch, hashing, search.
 * Main purpose is to provide efficiency immutable prefetched cache for messages in IMAP folder to obtain information
 * for example on deleted messages http://stackoverflow.com/questions/27565495/getting-uid-of-deleted-message/27573410?noredirect=1#comment45141607_27573410
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info>
 * @created 10-02-2015 12:06 AM
 **/
@Log4j2
class MessagesCache{
	final IMAPFolder folder;

	final Map<Long,IMAPMessage> uids;
	final Map<String,IMAPMessage> hashes;
	/**
	 * API Doc <a href="https://javamail.java.net/nonav/docs/api/javax/mail/MessageRemovedException.html">says</a>:
	 * The exception thrown when an invalid method is invoked on an expunged Message. The only valid methods on an expunged Message are isExpunged() and getMessageNumber().
	 * As last resort - if there no UID available, according to documentation
 	 */
	final Map<Integer,IMAPMessage> msgNumbers;

	MessagesCache(IMAPFolder folder) {
		this.folder = folder
		reopenFolderReadWrite();
		uids = folder.messages.collectEntries{Message m->
			[ ( m.getUIDsafe() ): m ]
		}.asImmutable();
		Map<String,List<IMAPMessage>> hashesList = folder.messages.groupBy{ it.sha1() }.asImmutable() as Map<String,List<IMAPMessage>>;

		// Check hashes unique! If they are not - we incorrect choose hash function or content
		assert hashesList*.value*.size().every{ 1 == it };
		hashes = hashesList*.value*.getAt(0) as Map<String,IMAPMessage>;

		msgNumbers = folder.messages.groupBy{ it.getMessageNumber() }.asImmutable() as Map<Integer,IMAPMessage>;
	}

	String messageToJson(IMAPMessage m, List<String> headers = [], boolean prettyPrint = false){
//???	assert m.getSize() == m.getMimeStream().text.size()
		Map essential
		if(m.expunged){ // Only base info and prefetched early data
			essential = extractMessageEssentials(m, headers);
			IMAPMessage mByUid = uids[m.getUIDsafe()];
			if (mByUid){
				essential.cachedMessageWithThatUIDwas = extractMessageEssentials(mByUid, headers);
			}
			IMAPMessage mByMessageNumber = msgNumbers[m.getMessageNumber()];
			if(mByMessageNumber){
				essential.cachedMessageWithThatMessageNumberWas = extractMessageEssentials(mByMessageNumber, headers);
			}
			if (mByUid && mByMessageNumber){
				assert mByUid.getUIDsafe() == mByMessageNumber.getUIDsafe()
				assert mByUid.getMessageNumber() == mByMessageNumber.getMessageNumber()
			}
		}
		else {
			essential = extractMessageEssentials(m, headers);
		}
		return (prettyPrint ? JsonOutput.prettyPrint(JsonOutput.toJson(essential)) : JsonOutput.toJson(essential));
	}

	@SuppressWarnings("GroovyAccessibility") // there many hacks to access private fields for deleted messages.
	protected static Map<String, Object> extractMessageEssentials(IMAPMessage m, List<String> headers) {
		if(m.expunged){ // Only base info and prefetched early data
			try{
				[
					DELETED: true
					,userFlags: m.@flags.userFlags // Direct field access without try refresh from server what lead to javax.mail.MessageRemovedException
					,UID: m.getUIDsafe()
					,getReceivedDate: { // Access private field instead of MessageRemovedException (can't use just m.@ notation because it java class) (http://stackoverflow.com/questions/1196192/how-do-i-read-a-private-field-in-java)
						Field f = IMAPMessage.getDeclaredField('receivedDate')
						f.setAccessible(true);
						f.get(m)
					}
					,getMessageNumber: m.getMessageNumber()
					,size: { // Access private field instead of MessageRemovedException (can't use just m.@ notation because it java class)
						Field f = IMAPMessage.getDeclaredField('size')
						f.setAccessible(true);
						f.get(m)
					}
					,sha1: m.lastSha1
				]
			}
			catch(MessageRemovedException rm){
				// @TODO Log incorrectly highlighted red due to the Idea bug: https://youtrack.jetbrains.com/issue/IDEA-136170
				log.error("MessageRemovedException happened on messageToJson() convert DELETED message with uid: ${m.getUIDsafe()}", rm);
			}
		}
		else{
			try{
				[
					userFlags                        : m.getFlags().getUserFlags()
					, UID                            : m.getUIDsafe()
					, getMessageID                   : m.getMessageID()
					, getModSeq                      : m.getModSeq()
					, getReceivedDate                : m.getReceivedDate()
					, getMessageNumber               : m.getMessageNumber()
					, getSequenceNumber              : m.getSequenceNumber()
					, size                           : m.getSize()
					, 'm.getMimeStream().text.size()': m.getMimeStream().text.size()
					, sha1                           : sha1(m.getMimeStream().text)
				]
				+
				m.getMatchingHeaders(headers as String[]).toList().collectEntries {
					["Header <${it.name}>": it.value]
				}
			}
			catch(MessageRemovedException rm){
				// @TODO Log incorrectly highlighted red due to the Idea bug: https://youtrack.jetbrains.com/issue/IDEA-136170
				log.error("MessageRemovedException happened on messageToJson() convert message with uid: ${m.getUIDsafe()}", rm);
			}
		}
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

		/**
		 * Return UID or null, no exception will be thrown
		 */
		IMAPMessage.metaClass.getUIDsafe = {
			try{
				(-1L == delegate.getUID() ? ((IMAPFolder)delegate.folder).getUID(delegate) : delegate.getUID())
			}
			catch (MessageRemovedException ignored){}
		}
	}

	void reopenFolderReadWrite(){
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
