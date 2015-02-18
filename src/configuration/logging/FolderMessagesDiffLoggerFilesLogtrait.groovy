package configuration.logging

import com.sun.mail.imap.IMAPMessage
import info.hubbitus.imaptree.utils.cache.MessagesCache

import java.lang.management.ManagementFactory

FolderMessagesDiffLoggerFiles { // Per trait (class)
	enabled = true;
	/**
	 * Closure.which return string by message. Used in perAnomaly and perMessage files
	 */
	messageShortPresentation = { MessagesCache cache, IMAPMessage m -> cache.messageToJson(m, ['X-message-unique', 'X-HeaderToolsLite', 'Date', 'Subject'], true) }

	/**
	 * Closure.which return string by full message representation.
	 */
	messageFullDump = { MessagesCache cache, IMAPMessage m ->
		String res;
		if(m.expunged) {
			res = 'MESSAGE DELETED!!!\n';
			res += 'Short representation instead: ' + cache.messageToJson(m, ['X-message-unique', 'X-HeaderToolsLite', 'Date', 'Subject'], true)
		} else return m.getMimeStream().text;

		res;
	}

	/**
	 * If folder does not exists - creation attempt performed
	 * Use closure to allow override and dynamically obtain directory value
	 */
	dir = '/home/pasha/Projects/ImapTreeSize/src/.results/diff';

	/**
	 * Two differences performed. Initial state. Than missed messages copied into folder2 and recheck
	 * performed. There second directory. No need use it anywhere in config - it just will be copied into dir.
	 */
	dirRecheck = '/home/pasha/Projects/ImapTreeSize/src/.results/diff.resultCheck';

	/**
	 * List of values:
	 * 	1 - first folder (source)
	 * 	2 - second (destination)
	 * 	false (null) - no one
	 *
	 * Off course you may dump both: [1, 2]
	 */
	dumpALLmessages = [1, 2];

	/**
	 * ANY filename closure may return Groovy false (0, null) and then will not performed attempt write it
	 * RECOMMENDED use defined before dir, but not required.
	 * It may be used as usual by full path like: log.diff.FolderMessagesDiffLoggerFiles.dir or just dir
	 * (${dir}), so per trait name binding will be provided.
	 */
	files {
		// Closure called at start. For example for clearing target dir.
		init = {
			// Clear directories only if they are from not current PID@host
			// http://snipplr.com/view/20787/
			String pid = ManagementFactory.getRuntimeMXBean().getName();
			try {
				if(new File("$dirRecheck/lock.pid").text == pid)
					return;
			}
			catch(FileNotFoundException ignore) {
			}

			println "Directories [$dir] and [$dirRecheck] will be deleted"
			new File(dir).deleteDir();
			new File(dirRecheck).deleteDir();
			new File(dirRecheck).mkdirs();
			new File("$dirRecheck/lock.pid").write(pid);
		}
		/**
		 * Short found differences summary file
		 */
		metricsCount = { "${dir}/_diff.summary" }

		/**
		 * By default files like: _folder1MessagesWithNonUniqueHashes.anomaly
		 * Now anomalies may be one of:
		 * folder1MessagesWithNonUniqueHashes, folder2MessagesWithNonUniqueHashes, messagesInFolder1ButNotInFolder2,
		 * messagesInFolder2ButNotInFolder1, messagesAdded (on folder2 listeners on copy), messagesRemoved (on folder2 listeners on copy),
		 * messagesChanged
		 * One mass file on problem: write set of messages if any
		 */
		perAnomaly = { String anomaly -> "${dir}/_${anomaly}.anomaly" }
		/**
		 * Each file on problem message. You should guarantee what name unique! Recommended use of UID or current dates
		 */
		perMessage = { String anomaly, IMAPMessage m -> "${dir}/_${anomaly}.msgs/${m.getUIDsafe()}-${m.getMessageNumber()}.msg" }
		/**
		 * File for dump full message for particular problem  with headers for deep analise
		 */
		dumpFullMessage = { String anomaly, IMAPMessage m ->
			"${dir}/_${anomaly}.msgs/${m.getUIDsafe()}-${m.getMessageNumber()}.eml"
		}

		/**
		 * Dump ALL messages (store locally)
		 * See before also ../dumpALLmessages option. first is folder number (1,2)
		 */
		dumpALLmessage = { Integer folderNo, IMAPMessage m ->
			"${dir}/_folder${folderNo}ALLmessages.msgs/${m.getUIDsafe()}-${m.getMessageNumber()}.eml"
		}

		/**
		 * If there differences found copy performed, and using UIDPLUS extension of IMAP protocol UIDs returned
		 */
		appendedUIDs = { "$dir/_appended.uids" }
	}
}