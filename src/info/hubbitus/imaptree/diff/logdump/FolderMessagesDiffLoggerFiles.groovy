package info.hubbitus.imaptree.diff.logdump

import groovy.util.logging.Log4j2
import com.sun.mail.imap.IMAPMessage
import info.hubbitus.imaptree.config.GlobalConf
import org.codehaus.groovy.control.io.NullWriter

import static info.hubbitus.imaptree.diff.FolderMessagesDiff.messageToJson

/**
 * Trait for store in filesystem hierarchy of folders and messages
 * Please look description of concept in {@see IFolderMessagesDiffLogger}
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-02-02 02:32
 **/
@Log4j2
trait FolderMessagesDiffLoggerFiles extends FolderMessagesDiffLoggerDefault{
	/**
	 * Override to provide another trait name
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @return
	 */
	ConfigObject getConf(){
//		GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.each{
//			if (Closure == it) it.delegate = GlobalConf.log.diff.FolderMessagesDiffLoggerFiles;
//		}
		GlobalConf.log.diff.FolderMessagesDiffLoggerFiles
	}

	/**
	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param messagesByHashes
	 * @param anomaly
	 */
	void diff_folderAnomaliesHelper(Map<String,List<IMAPMessage>> messagesByHashes, String anomaly){
//		log.debug("TEST: ${getClass()} CONF ${conf}")

		if (conf.enabled && messagesByHashes){
//			conf.files.perAnomaly.delegate = conf; // For resolve $dir if used
			withConfiguredWriter(conf.files.perAnomaly(anomaly)){Writer writer ->
				write_diff_folderAnomaly(writer, messagesByHashes, anomaly)
			}
		}
		super.diff_folderAnomaliesHelper(messagesByHashes, anomaly); // chain
	}

	/**
	 * If configuredFileName is groovy-true - write data into file with that name.
	 * Safe for exception writer flush and closing.
	 *
	 * @param configuredFileName Filename to write. Groovy's false to do not perform write at all
	 * @param write Closure to perform actual write. Should accept Writer argument.
	 */
	private static void withConfiguredWriter(String configuredFileName, Closure write){
		if(configuredFileName){
			new File(configuredFileName).parentFile.mkdirs(); // Try create all sub-dirs
			new File(configuredFileName).withWriter{Writer writer->
//				writer.write('TTTT')
				write(writer);
			}
		} else {
			write(new NullWriter());
		}
	}


	/**
	 * Write FULL anomaly into provided fullWriter of folder differences (list of messages by hash)
	 *
	 * @param messagesByHashes
	 * @param perAnomalyWrite
	 * @return
	 */
	private Map<String, List<IMAPMessage>> write_diff_folderAnomaly(Writer fullWriter, Map<String, List<IMAPMessage>> messagesByHashes, String anomaly) {
		String header = "=== $anomaly (${messagesByHashes.size()}) ===";
		fullWriter.println(header)
		messagesByHashes.each { String sha1, List<IMAPMessage> messages ->
			String hashSubHeader = "${sha1}(${messages.size()})";
			fullWriter.write(hashSubHeader);

			// per message
			messages.each{IMAPMessage m ->
//				conf.files.perMessage.delegate = conf;
				withConfiguredWriter(conf.files.perMessage(anomaly, m)){Writer writer ->
					writer.println(header);
					writer.println(hashSubHeader);

					String message = conf.messageShortPresentation(m);
					writer.println(message);
					fullWriter.println(message); // @TODO do it in parallel may increase performance
				}
//				conf.files.dumpFullMessage.delegate = conf;
				withConfiguredWriter(conf.files.dumpFullMessage(anomaly, m)) {Writer writer ->
					// Do not full body load if no real write needed
					if (writer != NullWriter) writer.println(m.getMimeStream().text);
				}
			}
		}
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param messagesByHashes
	 * @param anomaly
	 */
	void diff_messagesAnomaliesHelper(Map<String,IMAPMessage> messagesByHashes, String anomaly){
		if (conf.enabled && messagesByHashes){
//			conf.files.perAnomaly.delegate = conf; // For resolve $dir if used
			log.debug("TEST1: ${conf.files.perAnomaly(anomaly)}")
			def ttt = 77;
			withConfiguredWriter(conf.files.perAnomaly(anomaly)){Writer writer->
				write_diff_messagesAnomaly(writer, messagesByHashes, anomaly)
			}
		}

		super.diff_messagesAnomaliesHelper(messagesByHashes, anomaly); // chain
	}

	/**
	 * Write FULL anomaly into provided fullWriter of messages in folder anomaly (single messages by hash?)
	 *
	 * @param messagesByHashes
	 * @param perAnomalyWrite
	 * @return
	 */
	private Map<String, IMAPMessage> write_diff_messagesAnomaly(Writer fullWriter, Map<String, IMAPMessage> messagesByHashes, String anomaly) {
		String header = "=== $anomaly (${messagesByHashes.size()}) ===";
		fullWriter.println(header);
		messagesByHashes.each{String sha1, IMAPMessage m->
//			conf.files.perMessage.delegate = conf;
			log.debug("TEST2: ${conf.files.perMessage(anomaly, m)}")
			withConfiguredWriter(conf.files.perMessage(anomaly, m)){Writer writer->
				writer.println(header);
				String message = conf.messageShortPresentation(m);
				fullWriter.println(message); // @TODO do it in parallel may increase performance
				writer.println(message);
			}

//			conf.files.dumpFullMessage.delegate = conf;
			withConfiguredWriter(conf.files.dumpFullMessage(anomaly, m)){Writer writer ->
				// Do not full body load if no real write needed
				if (writer != NullWriter) writer.println(m.getMimeStream().text);
			}
		}
	}
}
