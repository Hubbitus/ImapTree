package info.hubbitus.imaptree.diff.logdump

import com.sun.mail.imap.AppendUID
import com.sun.mail.imap.IMAPMessage
import groovy.transform.CompileStatic
import groovy.transform.Memoized
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
	@Memoized // @Memoized by fact ignored for trait due to the bug https://jira.codehaus.org/browse/GROOVY-7293
	ConfigObject getConf(){
		// Rebind local variables: https://github.com/Hubbitus/groovy-test-examples/blob/03b6394299650078637570847e1ac363934ddd30/Structures-Patterns/ConfigSlurper/InConfigValuesAccess.groovy
		GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.each{key, value->
			if (value instanceof Closure)
				value.binding = new Binding(GlobalConf.log.diff.FolderMessagesDiffLoggerFiles);
		}
		GlobalConf.log.diff.FolderMessagesDiffLoggerDefault
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 */
	void diff_init() {
	}
/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param diff
	 */
	void diff_metricsCount(Map diff) {
		log.debug(diff);
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

	static String AppendUIDtoString(AppendUID uid){
		return "{uid:${uid.uid},uidvalidity:${uid.uidvalidity}}"
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 */
	void diff_appendedUIDs(AppendUID[] uids){
		log.debug('Appended UIDs: ' + uids.collect{ AppendUIDtoString(it) });
	}
}
