package info.hubbitus.imaptree.diff.logdump;

import com.sun.mail.imap.IMAPMessage;

import java.util.List;
import java.util.Map;

/**
 * Implementing traits chaining.
 * Look example as: {@see https://github.com/Hubbitus/groovy-test-examples/commit/c5f82b1e9246a3ef1dafae9484db67364e2d6ab3}
 *
 * Two base implementation provided:
 *	1st, base {@see FolderMessagesDiffLoggerDefailt} which gust dump provided content via log.debug
 *	2nd, filesystem based - store their diff into provided base dir into filesystem like IMAP folders and write there
 *		bunch of metadata files like (_folder1MessagesWithNonUniqueHashes.diff, _folder1MessagesWithNonUniqueHashes.diff_1,
 *		_folder1MessagesWithNonUniqueHashes.diff_2… and the messages). Please look detailed layout and exported data
 *		description in particular trait.
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-02-02 03:09
 */
interface IFolderMessagesDiffLogger {

	/**
	 * Returns sub-config related to that particular trait logging method from GlobalConf
	 * @TODO is it possible obtain in runtime trait name??
	 *
	 * @return
	 */
	ConfigObject getConf();

	void diff_folder1MessagesWithNonUniqueHashes(Map<String,List<IMAPMessage>> messagesByHashes);

	void diff_folder2MessagesWithNonUniqueHashes(Map<String,List<IMAPMessage>> messagesByHashes);

	void diff_messagesInFolder1ButNotInFolder2(Map<String,IMAPMessage> messagesByHashes);

	void diff_messagesInFolder2ButNotInFolder1(Map<String,IMAPMessage> messagesByHashes);
}
