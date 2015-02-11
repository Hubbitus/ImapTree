#!/opt/groovy-2.3.6/bin/groovy
import com.sun.mail.imap.IMAPFolder
@Grab(group = 'commons-cli', module = 'commons-cli', version = '1.2')

// TMP
@Grab(group = 'com.sun.mail', module = 'javax.mail', version = '1.5.2')
@Grab(group = 'com.sun.mail', module = 'gimap', version = '1.5.2')

@Grab(group = 'org.apache.logging.log4j', module = 'log4j-api', version = '2.1')
@Grab(group = 'org.apache.logging.log4j', module = 'log4j-core', version = '2.1')
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.CliBuilderAutoWidth
import info.hubbitus.imaptree.config.GlobalConf
import info.hubbitus.imaptree.config.ImapAccount
import info.hubbitus.imaptree.diff.FolderMessagesCopier
import info.hubbitus.imaptree.diff.FolderMessagesDiff
import info.hubbitus.imaptree.utils.cache.memcached.ImapTreeTranscoder
import info.hubbitus.imaptree.utils.cache.memcached.MemcachedClientExtended
import java.util.logging.Level

import java.util.concurrent.TimeUnit

/**
 * Simple example to just print folder sizes recursively
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 23.01.2015 23:30
 **/

def cli = new CliBuilderAutoWidth(/*usage: 'Usage:'*/)
cli.h(longOpt: 'help', 'This usage information', required: false)
cli.D(longOpt: 'config', '''Change configured options from command line. Allow runtime override. May appear multiple times - processed in that order. For example:
	-D log.fullXmlCache="some.file" --config operations.printFolderSizes.folderProcess='{true}' -D operations.printFolderSizes.messageProcess='{m-> println "SUBJ: ${m.subject}"}' --config "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
	Values trimmed - use quotes and escapes where appropriate''', required: false, args: 2, valueSeparator: '=', argName: 'property=value')
OptionAccessor opt = cli.parse(args)
GlobalConf.opt = opt;

if(opt.h /*|| opt.arguments().isEmpty()*/ ) {
	cli.usage()
}
else {
//println GlobalConf.log.test
	GlobalConf.overrideFromListPropertiesPairs(opt.Ds)

//println "GlobalConf.log.test=" + GlobalConf.log.test
//GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.perAnomaly.binding = new Binding(GlobalConf.log.diff.FolderMessagesDiffLoggerFiles)
//println "GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dir=${GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dir}"
//println "GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.perAnomaly('QWERTY')=${GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.perAnomaly('QWERTY')}"
//
//GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dir = '/root/'
//
//println "GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dir=${GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dir}"
//println "GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.perAnomaly('QWERTY')=${GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.files.perAnomaly('QWERTY')}"
//System.exit(101);

// use memcached to do not long await start and information gathering:
def mem = new MemcachedClientExtended(new InetSocketAddress('127.0.0.1', 11211));

ImapTreeSize tree1 = mem.getOrCreate('tree1', new ImapTreeTranscoder((ImapAccount)GlobalConf.accounts.Ant)) {
	new ImapTreeSize(GlobalConf.accounts.Ant)
};
ImapTreeSize tree2 = mem.getOrCreate('tree2', new ImapTreeTranscoder((ImapAccount)GlobalConf.accounts.PahanTest)) {
	new ImapTreeSize(GlobalConf.accounts.PahanTest)
};

((IMAPFolder)tree2.tree.@folder).store.session.setDebug(true);
((IMAPFolder)tree2.tree.@folder).store.session.setDebugOut(System.err);
((IMAPFolder)tree2.tree.@folder).store.session.logger.logger.level = Level.ALL

FolderMessagesDiff foldersDiff = new FolderMessagesDiff(tree1.tree.@folder, tree2.tree.@folder);
foldersDiff.dump('Before copy missed messages');

// Change path to do not overwrite. It must be before copier created because it use config for log changes in destination
GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dir = GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dirRecheck
FolderMessagesCopier copier = new FolderMessagesCopier(foldersDiff);
copier.copyMissedMessagesToFolder2();

// Recheck results
new FolderMessagesDiff(foldersDiff.folder1messages.folder, foldersDiff.folder2messages.folder).dump('After copy missed messages');

// Wait run message change listener thread
ThreadGroup mainGroup = Thread.currentThread().threadGroup;
Thread[] activeThreads = new Thread[mainGroup.activeCount()];
mainGroup.enumerate(activeThreads, true);
activeThreads.findAll{ it != Thread.currentThread() }*.join(10000); // Wait all at least 5 seconds

println 'Done'
// To do not hang
mem.shutdown(10, TimeUnit.MILLISECONDS);
}
