package info.hubbitus.imaptree

import com.sun.mail.imap.IMAPStore
@Grab(group='com.sun.mail', module='javax.mail', version='1.5.2')
@Grab(group='com.sun.mail', module='gimap', version='1.5.2')

@Grab(group='org.apache.logging.log4j', module='log4j-api', version='2.1')
@Grab(group='org.apache.logging.log4j', module='log4j-core', version='2.1')

@Grab(group='com.thoughtworks.xstream', module='xstream', version='1.4.7')

import groovy.util.logging.Log4j2
import com.thoughtworks.xstream.annotations.XStreamOmitField
import com.sun.mail.util.MailLogger
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.core.util.QuickWriter
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter
import com.thoughtworks.xstream.io.xml.StaxDriver
import info.hubbitus.imaptree.config.ImapAccount
import info.hubbitus.imaptree.config.Operation
import info.hubbitus.imaptree.utils.bench.ProgressLogger
import info.hubbitus.imaptree.xstream.DontSaveConverter

import javax.mail.*

/**
 * Recursively walk through IMAP folder from provided start and calculate that's size (number of messages and bytes)
 *
 * Base example from: http://dmitrijs.artjomenko.com/2012/03/how-to-read-imap-email-with-groovy.html
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info>
 * @created 2015-01-01 17:49
 */
@Log4j2
class ImapTreeSize {
	ImapAccount account;

	@Lazy Node tree = {
		Folder rootImapFolder = store.getFolder(account.folder);
		new Node(null, rootImapFolder.fullName, [root: true], rootImapFolder);
	}();

	@XStreamOmitField
	@Lazy private Store store = {
		//noinspection GroovyConstructorNamedArguments
		Session session = Session.getInstance(
			new Properties(
				'mail.store.protocol': account.type
				, 'mail.imaps.host': account.host
				, 'mail.imaps.port': account.port
			)
			, null
		);
		Store store = session.getStore(account.type);
		store.connect(account.host, account.login, account.password);
		store
	}();

	@XStreamOmitField
	private ProgressLogger pl;

	/**
	 *
	 * @param account
	 * @param doNotBuildTree For factory - to create object to what tie new de-serialised
	 */
	ImapTreeSize(ImapAccount account, boolean doNotBuildTree = false){
		this.account = account;

		if (!doNotBuildTree){
			buildTree();
			pl.stop();// Just for logging
		}
	}

	ImapTreeSize(String host, Integer port, String login, String password, String accountType, String folder = 'INBOX') {
		this(new ImapAccount(host, port, login, password, accountType, folder));
	}

	protected void buildTree(Folder cur = null, Node parentTreeNode = null) {
		Node node;

		if(null == cur) {
			pl = new ProgressLogger(-1, log.&info, null, 'Folder');
			cur = (Folder) tree.value();
			parentTreeNode = tree;
			parentTreeNode.value = cur;
			parentTreeNode.attributes().root = true;
			parentTreeNode.attributes().size = getFolderSize(cur);
			parentTreeNode.attributes().folder = cur;
			// Folder self. Value object of node became NodeList if some childs added
			node = parentTreeNode;
		} else {
			node = new Node(parentTreeNode, cur.fullName, [size: getFolderSize(cur), folder: cur], cur);

			log.debug """Folder <<${node.name()}>>: size: ${node.@size}; parent folder <<${
				//noinspection GroovyAssignabilityCheck
				node.parent().value()[0] // value()[0] required to obtain real value but not NodeList. Bug? See trees.groovy
			}>> size: ${node.parent().@size}"""
		}
		pl.next();

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

				return new Size(bytes: (Long) messages.collect { it.size }.sum(), messages: messages.size())
			}
			finally {
				f.close(false)
			}
		} else return new Size(onlyFolders: true, bytes: 0, messages: 0)
	}

	// Store text as CDATA sections for readability
	@XStreamOmitField
	@Lazy private XStream xStream = {
		XStream xStream = new XStream(
			new StaxDriver() { // For console run
				public HierarchicalStreamWriter createWriter(Writer out) {
					return new PrettyPrintWriter(out) {
						protected void writeText(QuickWriter writer, String text) {
							if(text ==~ /(?s).*[<>&].*/) {
								writer.write('<![CDATA[');
								writer.write(text);
								writer.write(']]>');
							} else {
								writer.write(text);
							}
						}
					};
				}
			}
		);

		xStream.autodetectAnnotations(true);
		xStream.classLoader = getClass().classLoader;
		xStream.omitField(ImapTreeSize, 'log');

//		xStream.omitField(Folder, 'store');
		xStream.registerConverter(new DontSaveConverter(Store, store));
//		xStream.omitField(IMAPFolder, 'logger');
		xStream.registerConverter(new DontSaveConverter(MailLogger, new MailLogger('_fromXmlFolder', 'DEBUG IMAP', store.session)));
//		xStream.omitField(IMAPFolder, 'connectionPoolLogger');
		xStream.registerConverter(new DontSaveConverter(MailLogger, ((IMAPStore)store).getConnectionPoolLogger()));

		xStream
	}();

	/**
	 * Return string representation of This object in XML. If that once saved to file that can be then reconstructed by
	 * fabric method {@link #deserialize(java.io.File, info.hubbitus.imaptree.config.ImapAccount)}}
	 *
	 * @return
	 */
	String serialize(){
		xStream.toXML(this);
	}

	/**
	 * Write string representation of This object in XML. If that once saved to file that can be then reconstructed by
	 * fabric method {@link #deserialize(java.io.File, info.hubbitus.imaptree.config.ImapAccount)}}
	 *
	 * File re-created if exists.
	 *
	 * @return
	 */
	void serializeToFile(File file) {
		if(file.exists()) file.delete();
		file << serialize();
	}

	/**
	 * Reconstruct object from previous saved XML cache file. {@see #serializeToFile ( java.io.File )}
	 *
	 * @param file
	 * @param imapAccount ImapAccount - for what tie deserialized object. Used to do not store excessive information like
	 *	store, loggers into file and leave it readable.
	 * @return
	 */
	static ImapTreeSize deserialize(File file, ImapAccount imapAccount) {
		ImapTreeSize imapTreeSize = new ImapTreeSize(imapAccount, true);
		imapTreeSize.deserialize_(file);
	}

	/**
	 * Reconstruct object from previous saved XML-string. {@see #serialize}
	 *
	 * @param file
	 * @param imapAccount ImapAccount - for what tie deserialized object. Used to do not store excessive information like
	 *	store, loggers into file and leave it readable.
	 * @return
	 */
	static ImapTreeSize deserialize(String str, ImapAccount imapAccount) {
		ImapTreeSize imapTreeSize = new ImapTreeSize(imapAccount, true);
		imapTreeSize.deserialize_(str);
	}

	/**
	 * Reconstruct object from previous saved XML cache file. {@see # serializeToFile (java.io.File)}
	 * Non-static helper for {@link #deserialize(java.io.File, info.hubbitus.imaptree.config.ImapAccount)}
	 *
	 * @param file
	 * @return
	 */
	private ImapTreeSize deserialize_(File file){
		xStream.fromXML(file);
	}

	private ImapTreeSize deserialize_(String str){
		xStream.fromXML(str);
	}

	/**
	 * Traverse all Folders and messages in it byt desired order
	 *
	 * @param folderHandle
	 * @param messageHandle
	 * @param traverseType
	 */
	void traverseTree(Operation operation){
		ProgressLogger.each(tree."${operation.treeTraverseOrder}"()){Node n ->
			try {
				if(operation.folderProcess(n)){
					operation.folderOpen(n);
					ProgressLogger.each(n.@folder.messages as List<Message>){Message message ->
						operation.messageProcess(message);
					} { log.info it }
				}
				if (operation.folderClose) operation.folderClose(n);
			}
			catch(Throwable t){
				log.fatal("Exception happened on processing operation: ", t);
			}
			finally {// Ensure folder closed
				if(n.@folder.open)
					n.@folder.close(false)
			}
		} { log.info it }
	}
}
