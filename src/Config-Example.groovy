import groovy.ui.Console
import info.hubbitus.imaptree.config.ImapAccount
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.Operation

import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message

// By default Config.groovy used as configuration! So, rename this file first, or adjust path in example scripts accordingly!!!
config{
	accounts{
		// You may define different accounts to choose later which use like:
		// ImapTreeSize imapTree = new ImapTreeSize(config.account1);
		// ImapTreeSize imapTree = new ImapTreeSize(config.account2);
		// In runtime you will be then choose it by -a/--account option like: --account account1
		account1 = new ImapAccount(
			host: 'imap.gmail.com'
			,port: 933
			,login: 'some.name@gmail.com'
			,password: 'Super-Pass'
			// Google Imap assumed. You may try use generic 'imap' or 'imaps' instead of 'gimaps', it should work but have not tested. Please let me known if you need it but it does not work
			,type: 'gimaps'
			,folder: 'INBOX' // Initial folder to scan.
		)
		// If you are good with defaults, you may use short form:
		account2 = new ImapAccount(
			login: 'some.name2@gmail.com'
			,password: 'Super-Pass for name 2'
		)
		// Some sort ov nesting available also with groovy ConfigSlurper magic:
		account3 = account1.with{
			login = 'some.name3@gmail.com'
			password = 'Super-Pass for name 3'
		}
		// and also constructor style form available (example same as account1):
		account4 = new ImapAccount('imap.gmail.com', 933, 'some.name@gmail.com', 'Super-Pass', 'gimaps', 'INBOX')
	}

	/**
	 * Operations to perform.
	 * One default operation defined historically - just print folder sizes,
	 *
	 */
	operations{
		// For description of options see Operation class
		printFolderSizes = new Operation(
			folderProcess: {Node node->
				println "printFolderSizes process folder ${node.@folder}"
				if (config.opt.'print-depth' && (node.name().split(node.@folder.separator.toString()).size() <= config.opt.'print-depth'.toInteger()) ){
					println "<<${node.name()}>>: SelfSize: ${node.@size}; subTreeSize: ${node.depthFirst().sum { it.@size }}; childSubtreeFolders: ${node.depthFirst().size() - 1}"
				}
				false
			}
			,messageProcess: {Message m-> } // Is not used in particular case (false returned from folder handler), but for example may contain: println m.subject
		)
		eachMessage = new Operation(
			folderProcess: {Node node->
				println "eachMessage process folder <<${node.name()}>>: SelfSize: ${node.@size}; subTreeSize: ${node.depthFirst().sum { it.@size }}; childSubtreeFolders: ${node.depthFirst().size() - 1}"
				true
			}
			,messageProcess: {Message m->
				println "<<${m.folder}>> (Folder attributes: ${m.folder.getAttributes()})); (Labels: ${m.getLabels()}); {SUBJ: ${m.subject}}"
			}
		)
		// Just bing results in interactive GUI GroovyConsole for further experiments
		GroovyConsole = new Operation(
			fullControl: {ImapTreeSize imapTree->
				// http://groovy.codehaus.org/Groovy+Console
				Console console = new Console([imapTree: imapTree, config: config] as Binding);
				console.run();
				console.with{ // Set default content of console
					swing.edt{
						inputArea.editable = false
					}
					swing.doOutside{
						try {
							def consoleText ='''// Typical usage this console to analyse results:
// All groovy magic available!
// F.e. redefine closure
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
		 * 2) If use Shift key or do not move messages into '[Gmail]/Trash' messages appeared in '[Gmail]All mail' and still eat space.
		 * 3) Deleting label from Gmail web-interface also leave messages in '[Gmail]All mail'
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
			,folderProcess: {Node node->
				true
			}
			,messageProcess: {Message m->
				println "gmailTrueGeleteMessages: <<${m.folder}>> (Folder attributes: ${m.folder.getAttributes()})); (Labels: ${m.getLabels()}); {SUBJ: ${m.subject}}"
				List labels = m.getLabels() - ''; // Strange bu always present empty '' label
				if (labels){ // If their any labels (current folder is not returned in list) - we just remove that - what is equal label remove - it should became in all other
					println "\tMessage has other labels ${labels} - regular delete it (remove label)"
					try{
						m.setFlag(Flags.Flag.DELETED, true);
					}
					catch(Exception e){
						println '!!!ERROR set DELETED flag on message: ' + e
					}
				}
				else{ // otherwise - move (copy) to [Gmail]/Trash folder to fully remove it, even from "[Gmail]/All mail" folder, because settings which are present in settings of account does not work as expected
					def trashFolder = m.folder.store.getFolder('[Gmail]').list('*').find{ /\Trash/ in it.getAttributes() } // [Gmail]/Корзина in Russian. Locale agnostic search by http://stackoverflow.com/a/26591696/307525
					println "\tMessage has no other labels, so move to '[Gmail]/Trash' ('${trashFolder}') to do not leave it also in '[Gmail]/All mail' (archive folder)"
					try{
						// http://www.jguru.com/faq/view.jsp?EID=1010890 - expunge in folder close later (IMAP have no move operation in base spec http://stackoverflow.com/questions/122267/imap-how-to-move-a-message-from-one-folder-to-another)
						m.folder.copyMessages((m as javax.mail.Message[]), trashFolder);
					}
					catch(Exception e){
						println '!!!ERROR copy message to [Gmail]/Trash folder: ' + e
					}
				}
			}
			,folderClose: {node->
				node.@folder.close(true); // Close and expunge
			}
		)
	}

	log{
		// To save full cache of results to operate next time from it. Provide false there to disable writing
		// When read from file occurred no any validation or invalidation performed (IMAP may change over time).
		// Directory must exists
		// %{account} will be replaced by used account name
		fullXmlCache = '.results/%{account}.data.xml'
	}
}