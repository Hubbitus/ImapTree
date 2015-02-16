import com.sun.mail.imap.IMAPMessage
import groovy.ui.Console
import groovyx.javafx.GroovyFX
import info.hubbitus.imaptree.config.ImapAccount
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.Operation
import info.hubbitus.imaptree.utils.cache.MessagesCache
import javafx.beans.property.ReadOnlyLongWrapper
import javafx.beans.property.ReadOnlyStringWrapper
import javafx.scene.control.Label
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeTableColumn
import javafx.scene.control.TreeTableView
import javafx.scene.layout.StackPane

import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import java.lang.management.ManagementFactory

// By default Config.groovy used as configuration! So, rename this file first, or adjust path in example scripts accordingly!!!
accounts {
	// You may define different accounts to choose later which use like:
	// ImapTreeSize imapTree = new ImapTreeSize(config.account1);
	// ImapTreeSize imapTree = new ImapTreeSize(config.account2);
	// In runtime you will be then choose it by -a/--account option like: --account account1
	account1 = new ImapAccount(
			name: 'FirstAccount'
			, host: 'imap.gmail.com'
			, port: 933
			, login: 'some.name@gmail.com'
			, password: 'Super-Pass'
			// Google Imap assumed. You may try use generic 'imap' or 'imaps' instead of 'gimaps', it should work but have not tested. Please let me known if you need it but it does not work
			, type: 'gimaps'
			, folder: 'INBOX' // Initial folder to scan.
	)
	// If you are good with defaults, you may use short form:
	account2 = new ImapAccount(
			name: 'For backup'
			, login: 'some.name2@gmail.com'
			// To read password from file for example.
			, password: 'cat .passfile'.execute().text
	)
	// Some sort ov nesting available also with groovy ConfigSlurper magic:
	account3 = account1.with {
		name = 'Account like 1st'
		login = 'some.name3@gmail.com'
		password = 'Super-Pass for name 3'
	}
	// and also constructor style form available (example same as account1):
	account4 = new ImapAccount('4th', 'imap.gmail.com', 933, 'some.name@gmail.com', 'Super-Pass', 'gimaps', 'INBOX')
}

/**
 * Operations to perform.
 * One default operation defined historically - just print folder sizes,
 *
 */
operations {
	// For description of options see Operation class
	printFolderSizes = new Operation(
			folderProcess: { Node node ->
				if((false == config.opt.'print-depth') || (node.name().split(node.@folder.separator.toString()).size() <= config.opt.'print-depth'.toInteger())) {
					println "<<${node.name()}>>: SelfSize: ${node.@size}; subTreeSize: ${node.depthFirst().sum { it.@size }}; childSubtreeFolders: ${node.depthFirst().size() - 1}"
				}
				false
			}
			, messageProcess: { Message m -> } // Is not used in particular case (false returned from folder handler), but for example may contain: println m.subject
	)
	eachMessage = new Operation(
			folderProcess: { Node node ->
				println "eachMessage process folder <<${node.name()}>>: SelfSize: ${node.@size}; subTreeSize: ${node.depthFirst().sum { it.@size }}; childSubtreeFolders: ${node.depthFirst().size() - 1}"
				true
			}
			// For Gmail there will be GmailMessage: https://javamail.java.net/nonav/docs/api/com/sun/mail/gimap/GmailMessage.html
			, messageProcess: { Message m ->
		println "<<${m.folder}>> (Folder attributes: ${m.folder.getAttributes()})); (Labels: ${m.getLabels()}); {SUBJ: ${m.subject}}"
	}
	)
	// Just bing results in interactive GUI GroovyConsole for further experiments
	GroovyConsole = new Operation(
			fullControl: { ImapTreeSize imapTree ->
				// http://groovy.codehaus.org/Groovy+Console
				// Start with current binding to allow access to existing classes http://stackoverflow.com/questions/16826014/embedded-groovy-console-how-do-i-share-entire-state
				Console console = new Console(imapTree.getClass().getClassLoader(), [imapTree: imapTree, config: config] as Binding);
				console.run();
				console.with { // Set default content of console
					swing.edt {
						inputArea.editable = false
					}
					swing.doOutside {
						try {
							def consoleText = '''// Typical usage this console to analyse results:
// All groovy magic available!
// F.e. redefine closure
config.operations.printFolderSizes.config = config
def operation = config.operations.printFolderSizes.clone()

operation.folderProcess = {node->
config.operations.printFolderSizes.folderProcess(node) // Call default handler, but also enable messages processing
true
}
// Add message processing:
operation.messageProcess = {m->
println "msg SUBJ: ${m.subject}"
}
imapTree.traverseTree(operation);

/* Did not want logging, error handling and prefer plain access? Never has been easy:
if (!imapTree.tree.@folder.open)
imapTree.tree.@folder.open(2 /*READ_WRITE*/)
println imapTree.tree.@folder.messages
*/
''';
							swing.edt {
								updateTitle()
								inputArea.document.remove 0, inputArea.document.length
								inputArea.document.insertString 0, consoleText, null
								setDirty(false)
								inputArea.caretPosition = 0
							}
						} finally {
							swing.edt { inputArea.editable = true }
							// GROOVY-3684: focus away and then back to inputArea ensures caret blinks
							swing.doLater outputArea.&requestFocusInWindow
							swing.doLater inputArea.&requestFocusInWindow
						}
					}
				}
			}
	)
	/**
	 * Task to really delete messages from selected folder, free space (do not leave it in '[Gmail]All mail' archive) and
	 * also do not delete copies from other folders (labels).
	 *
	 * GMail have special settings in options how to handle deleted via IMAP messages. But unfortunately it fully ignored.
	 * See file devel/Delete.Tests for more details and experiments.
	 * Handling IMAP actions also specific: https://support.google.com/mail/answer/77657?hl=en
	 * Really there not so many possibilities:
	 * 1) Setup move messages to '[Gmail]/Trash' folder and delete messages *without* hold Shift key - then messages fully
	 * deleted from ALL folders if there was copies.
	 * 1.1) It does not work with folders - labels like '[Gmail]/Корзина/My deleted folder' created instead.
	 * 2) If use Shift key or do not move messages into '[Gmail]/Trash' messages appeared in '[Gmail]/All mail' and still eat space.
	 * 3) Deleting label from Gmail web-interface also leave messages in '[Gmail]/All mail'
	 *
	 * So this task solve that problem and work in next way:
	 * 1) For configured root folder walk recursively and for each message:
	 * 2) If it present in more than just that folder - just remove from that folder (delete label)
	 * 3) If it does not present in any other folders - move it into '[Gmail]/Trash' (localized version should be handled)
	 * correctly.
	 * Essentially space from messages must be freed.
	 *
	 * Folders leaved as is to examine its empty.
	 **/
	gmailTrueDeleteMessages = new Operation(
			folderOpenMode: Folder.READ_WRITE
			, folderProcess: { Node node ->
		true
	}
			, messageProcess: { Message m ->
		println "gmailTrueGeleteMessages: <<${m.folder}>> (Folder attributes: ${m.folder.getAttributes()})); (Labels: ${m.getLabels()}); {SUBJ: ${m.subject}}"
		List<String> labels = (m.getLabels() - '').findAll{ !it.startsWith('\\') }; // Strange bu always present empty '' label, and labels like '\Important' also ignore
		if(labels) {
			// If their any labels (current folder is not returned in list) - we just remove that - what is equal label remove - it should became in all other
			println "\tMessage has other labels ${labels} - regular delete it (remove label)"
			try {
				m.setFlag(Flags.Flag.DELETED, true);
			}
			catch(Exception e) {
				println '!!!ERROR set DELETED flag on message: ' + e
			}
		} else {
			// otherwise - move (copy) to [Gmail]/Trash folder to fully remove it, even from "[Gmail]/All mail" folder, because settings which are present in settings of account does not work as expected
			def trashFolder = m.folder.store.getFolder('[Gmail]').list('*').find { /\Trash/ in it.getAttributes() }
			// [Gmail]/Корзина in Russian. Locale agnostic search by http://stackoverflow.com/a/26591696/307525
			println "\tMessage has no other labels, so move to '[Gmail]/Trash' ('${trashFolder}') to do not leave it also in '[Gmail]/All mail' (archive folder)"
			try {
				// http://www.jguru.com/faq/view.jsp?EID=1010890 - expunge in folder close later (IMAP have no move operation in base spec http://stackoverflow.com/questions/122267/imap-how-to-move-a-message-from-one-folder-to-another)
				m.folder.copyMessages((m as Message[]), trashFolder);
			}
			catch(Exception e) {
				println '!!!ERROR copy message to [Gmail]/Trash folder: ' + e
			}
		}
	}
			, folderClose: { node ->
		node.@folder.close(true); // Close and expunge
	}
	)
	/**
	 * Task to present gathered Imap-tree sizes as FX TreeTableView (
	 * 	https://docs.oracle.com/javase/8/javafx/user-interface-tutorial/tree-table-view.htm
	 * 	,https://wikis.oracle.com/display/OpenJDK/TreeTableView+User+Experience+Documentation)
	 *
	 * JavaFX is yong but very powerful!
	 * Even in that very simple operation representation you may select desired set of columns, sort, fold/unfold
	 * sub-trees, select lines and so on. I think it is the best instrument just for investigation like "where my space"
	 *
	 * 	Unfortunately there also some quirks:
	 * 	1) JavaFX is not fully and true open source yet. Most sources in http://openjdk.java.net/projects/openjfx/,
	 * 	but included in OpenJDK version does not work for me. Oracle JDK does.
	 * 	2) There used modern TreeTableView component, which is implemented only in Java-8. So only Oracle Java 8 is
	 * 	now single choose to use it.
	 * 	3) Even in Oracle JDK JavaFX still treated as non-mainstream, so not included in main classpath (placed in ext)
	 * 	and for run such operation you may try:
	 * 	3.1) Add it by run simple command as described: http://zenjava.com/javafx/maven/fix-classpath.html
	 * 		(http://stackoverflow.com/questions/14095430/how-to-grab-a-dependency-and-make-it-work-with-intellij-project)
	 * 	3.2) Or manually provide path to it on script running time:
	 * 	$ groovy -cp $JAVA_HOME/jre/lib/ext/jfxrt.jar:/home/pasha/.groovy/grapes/org.codehaus.groovyfx/groovyfx/jars/groovyfx-0.4.0.jar ./ImapTree.groovy --account BackupTest --operation fxTreeTable -c
	 * 	4) In that project also GroovyFX used, and due to reported by me bug:
	 * 	https://jira.codehaus.org/browse/GFX-41 we can't automatically Grab 0.4.0 version which is required. As
	 * 	workaround also you may provide path to .jar file manually as shown before.
	 * I hope such issues will ba vanished in time.
	 * Until it is not, and to do not make such dependencies mandatory, task written to use anywhere fully qualified
	 * class names to avoid imports, which may lead to just do not start even other tasks!
	 */
	fxTreeTable = new Operation(
			fullControl: { ImapTreeSize imapTree ->
				// Runtime manually grab to do not make it hard dependency. All classes use full qualified names and
				// does not imported for that reason
//				@Grab(group = 'org.codehaus.groovyfx', module = 'groovyfx', version = '0.3.1')
				// Until BUG https://jira.codehaus.org/browse/GFX-41 resolved not 0.4.0 version
//				groovy.grape.Grape.grab(group: 'org.codehaus.groovyfx', module:'groovyfx', version:'0.3.1')
				GroovyFX.start {
					stage(title: 'IMAP Tree folder sizes', visible: true) {
						scene(fill: BLACK, width: 700, height: 300) {
							// Unfortunately GroovyFX have no TreeTableView builder (yet?), so build it manually and insert as node
							node new TreeTableView(new TreeItem(imapTree.tree)).with {
								it.root.expanded = true;
								it.tableMenuButtonVisible = true;

								imapTree.tree.@fxItem = it.root;
								imapTree.tree.depthFirst().tail().each { Node n -> // Except 1st root
									n.@fxItem = new TreeItem(n); // Store for childs
									n.parent().@fxItem.children.add(n.@fxItem);
								}

								it.columns.setAll(
										new TreeTableColumn('Folder name').with {
//										it.setPrefWidth(150);
											it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
												new ReadOnlyStringWrapper(param.value.value.@folder.name) // Size object
											}
											it
										}
										, new TreeTableColumn('Folder full name').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyStringWrapper(param.value.value.name()) // Size object
									}
									it
								}
										, new TreeTableColumn('Self size, bytes').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyLongWrapper(param.value.value.@size.bytes ?: 0)
									}
									it
								}
										, new TreeTableColumn('Self size, hr').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyStringWrapper(param.value.value.@size.hr())
									}
									it
								}
										, new TreeTableColumn('Messages').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyLongWrapper(param.value.value.@size.messages ?: 0)
									}
									it
								}
										, new TreeTableColumn('Sub-tree size, bytes').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyLongWrapper(param.value.value.depthFirst().sum {
											it.@size
										}.bytes ?: 0)
									}
									it
								}
										, new TreeTableColumn('Sub-tree size, hr').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyStringWrapper(param.value.value.depthFirst().sum {
											it.@size
										}.hr())
									}
									it
								}
										, new TreeTableColumn('Sub-tree messages').with {
//										it.setPrefWidth(150);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyLongWrapper(param.value.value.depthFirst().sum {
											it.@size
										}.messages ?: 0)
									}
									it
								}
										, new TreeTableColumn('Child sub-tree folders').with {
//										it.setPrefWidth(80);
									it.cellValueFactory = { TreeTableColumn.CellDataFeatures<Node, String> param ->
										new ReadOnlyLongWrapper(param.value.value.depthFirst().size() - 1) // Size object
									}
									it
								}
								);
								// http://stackoverflow.com/questions/10952111/javafx-2-0-table-with-multiline-table-header
								it.columns.forEach { col ->
									Label label = new Label(col.getText());
									label.setStyle('-fx-padding: 0px; -fx-font: 10px "Serif"; -fx-text-alignment: center; -fx-min-width: 4em;'); // http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html
									label.setWrapText(true);

									StackPane stack = new StackPane();
									stack.getChildren().add(label);
									stack.prefWidthProperty().bind(col.widthProperty().subtract(5));
									label.prefWidthProperty().bind(stack.prefWidthProperty());
									col.setGraphic(stack);
								}
								it.columnResizePolicy = it.CONSTRAINED_RESIZE_POLICY
								it
							}
						}
					}
				}
			}
	)
}

cache{
	// @TODO move fullXmlCache file there
	xml{

	}

	/**
	 * To use it you should install, configure and run memcached server. As it very fast it is very useful for several
	 * subsequent experiments (debug) with assumption what underlined data has not be changed.
	 * NOTE - no automatic invalidation happened, so enable it use, disable and refill fully on you choose!
	 * If it enabled by --memcached and there present actual (not expired) information for that name of account it will be used. Otherwise gathering on IMAP performed and such cache filled.
	 * You may want use <a href="http://www.alphadevx.com/a/90-Accessing-Memcached-from-the-command-line">command line client</a> for example.
	 */
	memcached{
		host = '127.0.0.1'
		port = 11211
		/**
		 * Seconds on what tree stored. After that it will be purged
		 */
		cachetime = 3600
	}
}

log{
	// To save full cache of results to operate next time from it. Provide false there to disable writing
	// %{account} will be replaced by used account name
	fullXmlCache = ".results/%{account}.data.xml"

	/**
	 * Enable JavaMail logging of IMAP
	 */
	imapdebug = true

	diff{
		FolderMessagesDiffLoggerFiles{ // Per trait (class)
			enabled = true;
			/**
			 * Closure.which return string by message. Used in perAnomaly and perMessage files
			 */
			messageShortPresentation = {MessagesCache cache, IMAPMessage m-> cache.messageToJson(m, ['X-message-unique', 'X-HeaderToolsLite', 'Date', 'Subject'], true)}

			/**
			 * Closure.which return string by full message representation.
			 */
			messageFullDump = {MessagesCache cache, IMAPMessage m->
				String res;
				if (m.expunged){
					res = 'MESSAGE DELETED!!!\n';
					res += 'Short representation instead: ' + cache.messageToJson(m, ['X-message-unique', 'X-HeaderToolsLite', 'Date', 'Subject'], true)
				}
				else return m.getMimeStream().text;

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
			 *	1 - first folder (source)
			 *	2 - second (destination)
			 *	false (null) - no one
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
			files{
				// Closure called at start. For example for clearing target dir.
				init = {
					// Clear directories only if they are from not current PID@host
					// http://snipplr.com/view/20787/
					String pid = ManagementFactory.getRuntimeMXBean().getName();
					try{
						if (new File("$dirRecheck/lock.pid").text == pid)
							return;
					}
					catch(FileNotFoundException ignore){}

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
				perAnomaly = {String anomaly-> "${dir}/_${anomaly}.anomaly"}
				/**
				 * Each file on problem message. You should guarantee what name unique! Recommended use of UID or current dates
				 */
				perMessage = {String anomaly, IMAPMessage m-> "${dir}/_${anomaly}.msgs/${m.getUIDsafe()}-${m.getMessageNumber()}.msg" }
				/**
				 * File for dump full message for particular problem  with headers for deep analise
				 */
				dumpFullMessage = {String anomaly, IMAPMessage m->
					"${dir}/_${anomaly}.msgs/${m.getUIDsafe()}-${m.getMessageNumber()}.eml"
				}

				/**
				 * Dump ALL messages (store locally)
				 * See before also ../dumpALLmessages option. first is folder number (1,2)
				 */
				dumpALLmessage = {Integer folderNo, IMAPMessage m->
					"${dir}/_folder${folderNo}ALLmessages.msgs/${m.getUIDsafe()}-${m.getMessageNumber()}.eml"
				}

				/**
				 * If there differences found copy performed, and using UIDPLUS extension of IMAP protocol UIDs returned
				 */
				appendedUIDs = { "$dir/_appended.uids" }
			}
		}
	}
}
