#!/opt/groovy-2.3.6/bin/groovy
import com.sun.mail.imap.IMAPFolder
import groovy.util.logging.Log4j2
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
import net.spy.memcached.MemcachedClient

import java.util.concurrent.TimeUnit
import java.util.logging.Level

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
cli.s(longOpt: 'account-src', 'Source account for proceed (account1)', required: true, args: 1)
cli.d(longOpt: 'account-dst', 'Destination account for proceed (account2)', required: true, args: 1)
cli.m(longOpt: 'memcached', 'Use memcached. See cache.memcached settings in config file. If there present actual (not expired) information for that name of account it will be used. Otherwise gathering on IMAP performed and such cache filled. Be careful, no any cache invalidation performed (for example if underlied Mail data changed of even settings)', required: false)
cli.c(longOpt: 'copy', 'Copy missed mails. Only Source->Destination copy performed.', required: false)
cli.r(longOpt: 'recheck', 'Implied --copy (does not run if no copy performed). Run again initial check. Now value of log.diff.FolderMessagesDiffLoggerFiles.dirRecheck will be used for log.diff.FolderMessagesDiffLoggerFiles.dirRecheck if such logging enabled at all.', required: false)

OptionAccessor opt = cli.parse(args)
GlobalConf.opt = opt;

@Log4j2
class ImapTreeDiffLog{}

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

	ImapTreeSize tree1, tree2;
	MemcachedClient mem;
	if (opt.m){
		// use memcached to do not long await start and information gathering:
		mem = new MemcachedClientExtended(new InetSocketAddress(GlobalConf.memcached.host, GlobalConf.memcached.port));
		ImapAccount accountSrc = GlobalConf.accounts[opt.'account-src'] as ImapAccount;
		ImapAccount accountDst = GlobalConf.accounts[opt.'account-dst'] as ImapAccount;
		tree1 = mem.getOrCreate("ImapTreeDiff.tree1.${accountSrc.name}", GlobalConf.memcached.cachetime, new ImapTreeTranscoder(accountSrc)) {
			new ImapTreeSize(accountSrc)
		} as ImapTreeSize;
		tree2 = mem.getOrCreate("ImapTreeDiff.tree2.${accountDst.name}", GlobalConf.memcached.cachetime, new ImapTreeTranscoder(accountDst)) {
			new ImapTreeSize(accountDst)
		} as ImapTreeSize;
	}
	else{
		tree1 = new ImapTreeSize((ImapAccount)GlobalConf.accounts[opt.'account-src'])
		tree2 = new ImapTreeSize((ImapAccount)GlobalConf.accounts[opt.'account-dst'])
	}


//((IMAPFolder)tree2.tree.@folder).store.session.setDebug(true);
//((IMAPFolder)tree2.tree.@folder).store.session.setDebugOut(System.err);
//((IMAPFolder)tree2.tree.@folder).store.session.logger.logger.level = Level.ALL

	FolderMessagesDiff foldersDiff = new FolderMessagesDiff(tree1.tree.@folder, tree2.tree.@folder);
	foldersDiff.dump('Before copy missed messages');

	// Change path to do not overwrite. It must be before copier created because it use config for log changes in destination
	GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dir = GlobalConf.log.diff.FolderMessagesDiffLoggerFiles.dirRecheck
	if(opt.copy){
		ImapTreeDiffLog.log.info('Copying requested. Run.');
		FolderMessagesCopier copier = new FolderMessagesCopier(foldersDiff);
		copier.copyMissedMessagesToFolder2();

		// Recheck results
		if (opt.recheck){
			ImapTreeDiffLog.log.info('Copying requested. Run.');
			new FolderMessagesDiff(foldersDiff.folder1messages.folder, foldersDiff.folder2messages.folder).dump('After copy missed messages');
		}

		// Wait run message change listener thread. Unfortunately Java finalize is not same as destructor, so it code outside
		ThreadGroup mainGroup = Thread.currentThread().threadGroup;
		Thread[] activeThreads = new Thread[mainGroup.activeCount()];
		mainGroup.enumerate(activeThreads, true);
		activeThreads.findAll{ it != Thread.currentThread() }*.join(5000); // Wait all at least 5 seconds
	}


	println 'Done'
	// To do not hang
	if(mem)
		mem.shutdown(10, TimeUnit.MILLISECONDS);
}
