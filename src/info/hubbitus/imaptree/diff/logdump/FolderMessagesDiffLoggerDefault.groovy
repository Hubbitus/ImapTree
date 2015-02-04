package info.hubbitus.imaptree.diff.logdump

import com.sun.mail.imap.IMAPMessage
import groovy.util.logging.Log4j2
import info.hubbitus.imaptree.config.GlobalConf

import static info.hubbitus.imaptree.diff.FolderMessagesDiff.messageToJson
import static info.hubbitus.imaptree.utils.errors.ErrorsProcessing.callerMethodName

/**
 * Trait to log message differences of IMAP folders just into log-file.
 * Please look description in {@see IFolderMessagesDiffLogger}
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-02-02 01:48
 **/
@Log4j2
trait FolderMessagesDiffLoggerDefault implements IFolderMessagesDiffLogger{
	/**
	 * Override to provide another trait name
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @return
	 */
	ConfigObject getConf(){
		GlobalConf.log.diff.FolderMessagesDiffLoggerDefault
	}

	/**
	 * May be protected in class, but not in trait
	 *
	 * @param messagesByHashes
	 * @param anomaly
	 */
	void diff_folderAnomaliesHelper(Map<String,List<IMAPMessage>> messagesByHashes, String anomaly){
		if (messagesByHashes){
			log.debug("ANOMALY: $anomaly (${messagesByHashes.size()}):")
			messagesByHashes.each{String sha1, List<IMAPMessage> messages->
				log.debug("${sha1}(${messages.size()})");
				messages.each{IMAPMessage m->
					log.debug(messageToJson(m, ['X-message-unique', 'X-HeaderToolsLite', 'Date'], true));
				}
			}
		}
	}

	void diff_messagesAnomaliesHelper(Map<String,IMAPMessage> messagesByHashes, String anomaly){
		if (messagesByHashes){
			log.debug("ANOMALY: $anomaly (${messagesByHashes.size()}):")
			messagesByHashes.each{String sha1, IMAPMessage m->
				log.debug(messageToJson(m, ['X-message-unique', 'X-HeaderToolsLite', 'Date'], true));
			}
		}
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param messagesByHashes
	 */
	void diff_folder1MessagesWithNonUniqueHashes(Map<String,List<IMAPMessage>> messagesByHashes){
		diff_folderAnomaliesHelper(messagesByHashes, callerMethodName())
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param messagesByHashes
	 */
	void diff_folder2MessagesWithNonUniqueHashes(Map<String,List<IMAPMessage>> messagesByHashes){
		diff_folderAnomaliesHelper(messagesByHashes, callerMethodName())
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param messagesByHashes
	 */
	void diff_messagesInFolder1ButNotInFolder2(Map<String,IMAPMessage> messagesByHashes){
		diff_messagesAnomaliesHelper(messagesByHashes, callerMethodName())
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param messagesByHashes
	 */
	void diff_messagesInFolder2ButNotInFolder1(Map<String,IMAPMessage> messagesByHashes){
		diff_messagesAnomaliesHelper(messagesByHashes, callerMethodName())
	}
}
