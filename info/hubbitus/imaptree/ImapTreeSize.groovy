package info.hubbitus.imaptree

//@Grab(group='javax.mail', module='mail', version='1.5.2')
@Grab(group='com.sun.mail', module='javax.mail', version='1.5.2')
@Grab(group='com.sun.mail', module='gimap', version='1.5.2')
@Grab(group='org.apache.logging.log4j', module='log4j-api', version='2.1')
@Grab(group='org.apache.logging.log4j', module='log4j-core', version='2.1')

import groovy.util.logging.Log4j2

import javax.mail.FetchProfile

// Base example from: http://dmitrijs.artjomenko.com/2012/03/how-to-read-imap-email-with-groovy.html
import javax.mail.Folder
import javax.mail.Message
import javax.mail.Session
import javax.mail.Store

/**
 * Recursively walk through IMAP folder from provided start and calculate that's size (number of messages and bytes)
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info>
 * @created 2015-01-01 17:49
 */
@Log4j2
class ImapTreeSize{
	Folder rootImapFolder;

	Node tree;

	private Store store;
	public static ConfigObject config = new ConfigObject();

	public ImapTreeSize(String host, Integer port, String login, String password, String folder = 'INBOX') {
		config.host = host;
		config.port = port;
		config.login = login;
		config.password = password;
		config.folder = folder;

		connect()
		buildTree();
	}

	protected void connect() {
		Session session = Session.getInstance(
			new Properties(
				'mail.store.protocol': 'imaps'
				, 'mail.imaps.host': 'imap.gmail.com'
				, 'mail.imaps.port': '993'
			)
			,null
		)

		store = session.getStore('gimaps') // GmailSSLStore actually
		store.connect(config.host, config.login, config.password)

		rootImapFolder = store.getFolder(config.folder) // GmailFolder
		tree = new Node(null, rootImapFolder.fullName, [root: true], rootImapFolder)
	}

	protected void buildTree(Folder cur = null, Node parentTreeNode = null) {
		Node node;

		if(null == cur) {
			cur = rootImapFolder;
			parentTreeNode = tree;
			parentTreeNode.value = cur;
			parentTreeNode.attributes().root = true;
			parentTreeNode.attributes().size = getFolderSize(cur);
			node = parentTreeNode;
		} else {
			node = new Node(parentTreeNode, cur.fullName, [size: getFolderSize(cur)], cur);
			log.debug """Folder <<${node.name()}>>: size: ${node.@size}; parent folder <<${node.parent().value()[0]}>> size: ${node.parent().@size}"""
			// value()[0] required to obtain real value but not NodeList. Bug? See trees.groovy
		}

		cur.list().each { // recursive
			buildTree(it, node)
		}
	}

	Size getFolderSize(Folder f) {
		if(f.type & f.HOLDS_MESSAGES) {
			try {
				log.debug "Calculate size of folder: <<$f>>; total messages: ${f.getMessageCount()}, deleted count: ${f.getDeletedMessageCount()}, new count: ${f.getNewMessageCount()}, unread count: ${f.getUnreadMessageCount()}";
				f.open(Folder.READ_ONLY);
				FetchProfile fp = new FetchProfile();
				fp.add(FetchProfile.Item.SIZE);
				List<Message> messages = f.messages as List;
				f.fetch(messages as Message[], fp);

				return new Size(bytes: messages.collect { it.size }.sum(), messages: messages.size())
			}
			finally {
				f.close(false)
			}
		} else return new Size(onlyFolders: true, bytes: 0, messages: 0)
	}
}
