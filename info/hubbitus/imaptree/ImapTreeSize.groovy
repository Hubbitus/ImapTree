package info.hubbitus.imaptree

import com.sun.mail.gimap.GmailFolder
import com.sun.mail.imap.IMAPFolder
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.annotations.XStreamOmitField
import com.thoughtworks.xstream.core.util.QuickWriter
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter
import com.thoughtworks.xstream.io.xml.StaxDriver
import com.thoughtworks.xstream.io.xml.XppDriver
@Grab(group='com.sun.mail', module='javax.mail', version='1.5.2')
@Grab(group='com.sun.mail', module='gimap', version='1.5.2')

@Grab(group='org.apache.logging.log4j', module='log4j-api', version='2.1')
@Grab(group='org.apache.logging.log4j', module='log4j-core', version='2.1')

@Grab(group='xmlpull', module='xmlpull', version='1.1.3.1')
@Grab(group='com.thoughtworks.xstream', module='xstream', version='1.4.7')

import groovy.util.logging.Log4j2

import javax.mail.*

//@Grab(group='javax.mail', module='mail', version='1.5.2')
/**
 * Recursively walk through IMAP folder from provided start and calculate that's size (number of messages and bytes)
 *
 * Base example from: http://dmitrijs.artjomenko.com/2012/03/how-to-read-imap-email-with-groovy.html
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info>
 * @created 2015-01-01 17:49
 */
@Log4j2
class ImapTreeSize{
	public ImapAccount account;

	Node tree;

	@XStreamOmitField
	private Store store;
	@XStreamOmitField
	private Folder rootImapFolder;

	ImapTreeSize(ImapAccount account){
		this.account = account;

		connect()
		buildTree();
	}

	ImapTreeSize(String host, Integer port, String login, String password, String accountType, String folder = 'INBOX') {
		this(new ImapAccount(host, port, login, password, accountType, folder));
	}

	protected void connect() {
		Session session = Session.getInstance(
			new Properties(
				'mail.store.protocol': account.type
				, 'mail.imaps.host': account.host
				, 'mail.imaps.port': account.port
			)
			,null
		);

		store = session.getStore(account.type);
		store.connect(account.host, account.login, account.password);

		rootImapFolder = store.getFolder(account.folder); // GmailFolder
		tree = new Node(null, rootImapFolder.fullName, [root: true], rootImapFolder);
	}

	protected void buildTree(Folder cur = null, Node parentTreeNode = null) {
		Node node;

		if(null == cur) {
			cur = rootImapFolder;
			parentTreeNode = tree;
			parentTreeNode.value = cur;
			parentTreeNode.attributes().root = true;
			parentTreeNode.attributes().size = getFolderSize(cur);
			parentTreeNode.attributes().folder = cur; // Folder self. Value object of node became NodeList if some childs added
			node = parentTreeNode;
		} else {
			node = new Node(parentTreeNode, cur.fullName, [size: getFolderSize(cur), folder: cur], cur);
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

	// Store text as CDATA sections for readability
	@Lazy
	@XStreamOmitField
	private static XStream xStream = {
		XStream xStream = new XStream(
//			new XppDriver() {
			new StaxDriver(){ // For console run
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
		xStream.omitField(Folder, 'store');
		xStream.omitField(IMAPFolder, 'logger');
		xStream.omitField(IMAPFolder, 'connectionPoolLogger');

		xStream
	}();

	/**
	 * Return string representation of This object. If that once saved to file that can be then reconstructed by fabric
	 * method {@link #fromCacheXMLfile(java.io.File)}
	 *
	 * @return
	 */
	String serializeToXML(){
		return xStream.toXML(this);
	}

	/**
	 *
	 * @param file
	 * @return
	 */
	public static fromCacheXMLfile(File file){
		return xStream.fromXML(file);
	}
}
