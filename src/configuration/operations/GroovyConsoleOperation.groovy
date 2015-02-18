package configuration.operations

import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.Operation

// Just bing results in interactive GUI GroovyConsole for further experiments
GroovyConsole = new Operation(
	fullControl: {ImapTreeSize imapTree->
		// http://groovy.codehaus.org/Groovy+Console
		Console console = new Console(imapTree.getClass().getClassLoader(), [imapTree: imapTree] as Binding);
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