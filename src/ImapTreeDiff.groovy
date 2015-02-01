#!/opt/groovy-2.3.6/bin/groovy
import com.sun.mail.imap.AppendUID
import com.sun.mail.imap.IMAPFolder
import com.sun.mail.imap.IMAPMessage
import groovy.json.JsonOutput
@Grab(group = 'commons-cli', module = 'commons-cli', version = '1.2')

// TMP
@Grab(group = 'com.sun.mail', module = 'javax.mail', version = '1.5.2')
@Grab(group = 'com.sun.mail', module = 'gimap', version = '1.5.2')

@Grab(group = 'org.apache.logging.log4j', module = 'log4j-api', version = '2.1')
@Grab(group = 'org.apache.logging.log4j', module = 'log4j-core', version = '2.1')
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.ImapAccount
import info.hubbitus.imaptree.diff.FolderMessagesCopier
import info.hubbitus.imaptree.diff.FolderMessagesDiff
import info.hubbitus.imaptree.utils.ConfigExtended
import info.hubbitus.imaptree.utils.cache.memcached.ImapTreeTranscoder
import info.hubbitus.imaptree.utils.cache.memcached.MemcachedClientExtended

import javax.mail.Folder
import java.security.MessageDigest
import javax.mail.event.MessageChangedListener
import javax.mail.event.MessageChangedEvent
import javax.mail.event.MessageCountListener
import javax.mail.event.MessageCountEvent

import static info.hubbitus.imaptree.diff.FolderMessagesDiff.messageToJson

/**
 * Simple example to just print folder sizes recursively
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 23.01.2015 23:30
 * */

// use memcached to do not long await start and information gathering:
def mem = new MemcachedClientExtended(new InetSocketAddress('127.0.0.1', 11211));

ConfigExtended config = (ConfigExtended) new ConfigSlurper().parse(Config).config;

ImapTreeSize tree1 = mem.getOrCreate('tree1', new ImapTreeTranscoder((ImapAccount)config.accounts.Ant)) {
	new ImapTreeSize(config.accounts.Ant)
};
ImapTreeSize tree2 = mem.getOrCreate('tree2', new ImapTreeTranscoder((ImapAccount)config.accounts.PahanTest)) {
	new ImapTreeSize(config.accounts.PahanTest)
};

FolderMessagesDiff foldersDiff = new FolderMessagesDiff(tree1.tree.@folder, tree2.tree.@folder);
foldersDiff.dump('Before copy missed messages');

FolderMessagesCopier copier = new FolderMessagesCopier(foldersDiff);
copier.copyMissedMessagesToFolder2();

new FolderMessagesDiff(foldersDiff).dump('After copy missed messages');

println 'Done'
// To do not hang
mem.shutdown();