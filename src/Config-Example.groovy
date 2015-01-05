import groovy.ui.Console
import info.hubbitus.imaptree.config.ImapAccount
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.Operation

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
					println "<<${node.name()}>>: SelfSize: ${node.@size}; treeSize: ${node.depthFirst().sum { it.@size }}; treeChilds: ${node.depthFirst().size()}"
				}
				false
			}
			,messageProcess: {Message m-> } // Is not used in particular case (false returned from folder handler), but for example may contain: println m.subject
		)
		eachMessage = new Operation(
			folderProcess: {Node node->
				println "eachMessage process folder <<${node.name()}>>: SelfSize: ${node.@size}; treeSize: ${node.depthFirst().sum { it.@size }}; treeChilds: ${node.depthFirst().size()}"
				true
			}
			,messageProcess: {Message m-> println "eachMessage msg SUBJECT: ${m.subject}"} // Is not used in particular case (false returned from folder handler), but for example may contain: println m.subject
		)
		// Just bing results in interactive GUI GroovyConsole for further experiments
		GroovyConsole = new Operation(
			fullControl: {ImapTreeSize imapTree, ConfigObject config->
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
	}

	log{
		// To save full cache of results to operate next time from it. Provide false there to disable writing
		// When read from file occurred no any validation or invalidation performed (IMAP may change over time).
		// Directory must exists
		// %{account} will be replaced by used account name
		fullXmlCache = '.results/%{account}.data.xml'
	}
}