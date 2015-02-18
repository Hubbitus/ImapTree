package configuration.operations

import info.hubbitus.imaptree.config.Operation

import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message

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
	,folderProcess: { Node node ->
		true
	}
	, messageProcess: { Message m ->
		println "gmailTrueGeleteMessages: <<${m.folder}>> (Folder attributes: ${m.folder.getAttributes()})); (Labels: ${m.getLabels()}); {SUBJ: ${m.subject}}"
		List<String> labels = (m.getLabels() - '').findAll {
			!it.startsWith('\\')
		}; // Strange bu always present empty '' label, and labels like '\Important' also ignore
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
			def trashFolder = m.folder.store.getFolder('[Gmail]').list('*').find {
				/\Trash/ in it.getAttributes()
			}// [Gmail]/Корзина in Russian. Locale agnostic search by http://stackoverflow.com/a/26591696/307525
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
	,folderClose: { node ->
		node.@folder.close(true); // Close and expunge
	}
)