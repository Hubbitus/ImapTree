package info.hubbitus.imaptree.diff.logdump

import groovy.transform.Memoized
import groovy.util.logging.Log4j2
import com.sun.mail.imap.AppendUID
import groovy.json.JsonOutput
import com.sun.mail.imap.IMAPMessage
import info.hubbitus.imaptree.config.GlobalConf
import info.hubbitus.imaptree.utils.errors.EvaluateClosureChecked
import org.codehaus.groovy.control.io.NullWriter

/**
 * Trait for store in filesystem hierarchy of folders and messages
 * Please look description of concept in {@see IFolderMessagesDiffLogger}
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-02-02 02:32
 **/
@Log4j2
trait FolderMessagesDiffLoggerFiles extends FolderMessagesDiffLoggerDefault implements EvaluateClosureChecked{
	/**
	 * Override to provide another trait name
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @return
	 */
	@Memoized // @TODO @Memoized by fact ignored for trait due to the bug https://jira.codehaus.org/browse/GROOVY-7293
	ConfigObject getConf(){
		// Rebind local variables: https://github.com/Hubbitus/groovy-test-examples/blob/03b6394299650078637570847e1ac363934ddd30/Structures-Patterns/ConfigSlurper/InConfigValuesAccess.groovy
		GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.each{key, value->
			if (value instanceof Closure)
				value.binding = new Binding(GlobalConf.log.diff.FolderMessagesDiffLoggerFiles);
		}
		GlobalConf.log.diff.FolderMessagesDiffLoggerFiles
	}

	/**
	 * Clear
	 *
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 */
	void diff_init() {
		log.debug("Call conf.files.init()");
		evaluateClosureChecked('conf.files.init()'){
			conf.files.init();
		}
		super.diff_init(); // chain
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 */
	void diff_metricsCount(Map diff){
		withConfiguredWriter(
			evaluateClosureChecked('conf.files.metricsCount()'){
				conf.files.metricsCount();
			} as String){Writer writer->
			writer.println(JsonOutput.prettyPrint(JsonOutput.toJson(diff)));
		}
		super.diff_metricsCount(diff); // chain
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 */
	void diff_appendedUIDs(AppendUID[] uids){
		withConfiguredWriter(
			evaluateClosureChecked('conf.files.appendedUIDs()'){
				conf.files.appendedUIDs();
			}
			){Writer writer->
			writer.println(JsonOutput.prettyPrint(JsonOutput.toJson(uids.collect{ AppendUIDtoString(it) })));
		}
		super.diff_appendedUIDs(uids); // chain
	}

	/**
	 * Due to the Groovy bug https://jira.codehaus.org/browse/GROOVY-7198 we can't use @Override
	 *
	 * @param messagesByHashes
	 * @param anomaly
	 */
	void diff_folderAnomaliesHelper(Map<String,List<IMAPMessage>> messagesByHashes, String anomaly){
		if (conf.enabled && messagesByHashes){
			withConfiguredWriter(
				evaluateClosureChecked("conf.files.perAnomaly($anomaly)"){
					conf.files.perAnomaly(anomaly);
				}
				){Writer writer ->
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
	synchronized private static void withConfiguredWriter(String configuredFileName, Closure write){
		if(configuredFileName){
			new File(configuredFileName).parentFile.mkdirs(); // Try create all sub-dirs
			new File(configuredFileName).withWriterAppend{Writer writer->
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
				withConfiguredWriter(
					evaluateClosureChecked("conf.files.perMessage($anomaly, m)"){
						conf.files.perMessage(anomaly, m);
					}){Writer writer ->
					writer.println(header);
					writer.println(hashSubHeader);

					String message = evaluateClosureChecked('conf.messageShortPresentation(m);') {
						conf.messageShortPresentation(m);
					}
					writer.println(message);
					fullWriter.println(message); // @TODO do it in parallel may increase performance
				}
				withConfiguredWriter(
					evaluateClosureChecked("conf.files.dumpFullMessage($anomaly, m)"){
						conf.files.dumpFullMessage(anomaly, m)
					}) {Writer writer ->
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
		log.debug('TEST1:' + (conf.enabled && messagesByHashes))
		log.debug('TEST1.1:' + conf.enabled)
		log.debug('TEST1.2:' + messagesByHashes)
		log.debug('TEST1.3:' + anomaly)
		log.debug('TEST1.4:' + messagesByHashes.size())
//?		log.debug('TEST1.5:' + messagesByHashes.collectEntries{String sha1, IMAPMessage m-> [ (sha1): conf.messageShortPresentation(m) ] })
		if (conf.enabled && messagesByHashes){
			String file = evaluateClosureChecked("conf.files.perAnomaly($anomaly)"){
				conf.files.perAnomaly(anomaly)
			}
			log.debug("diff_messagesAnomaliesHelper [$anomaly], write to [${file}]")
			withConfiguredWriter(file){Writer writer->
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
			String file = evaluateClosureChecked("conf.files.perMessage($anomaly, m)"){
				conf.files.perMessage(anomaly, m);
			}
			log.debug("write_diff_messagesAnomaly [$anomaly], write to [${file}]")
			withConfiguredWriter(file){Writer writer->
				writer.println(header);
				String message = evaluateClosureChecked('conf.messageShortPresentation(m)'){
					conf.messageShortPresentation(m);
				}
				fullWriter.println(message); // @TODO do it in parallel may increase performance
				writer.println(message);
			}

			file = evaluateClosureChecked("conf.files.dumpFullMessage($anomaly, m)"){
				conf.files.dumpFullMessage(anomaly, m);
			}
			log.info('TEST2:' + file);
			log.info('TEST2.1:' + anomaly);
			withConfiguredWriter(file){Writer writer ->
				if (writer != NullWriter) writer.println(m.getMimeStream().text);
			}
		}
	}
}
