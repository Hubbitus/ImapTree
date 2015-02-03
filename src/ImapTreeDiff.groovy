#!/opt/groovy-2.3.6/bin/groovy
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

/**
 * Simple example to just print folder sizes recursively
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 23.01.2015 23:30
 * */

def cli = new CliBuilderAutoWidth(/*usage: 'Usage:'*/)
cli.h(longOpt: 'help', 'This usage information', required: false)
cli.D(longOpt: 'config', '''Change configured options from command line. Allow runtime override. May appear multiple times - processed in that order. For example:
	-D log.fullXmlCache="some.file" --config operations.printFolderSizes.folderProcess='{true}' -D operations.printFolderSizes.messageProcess='{m-> println "SUBJ: ${m.subject}"}' --config "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
	Values trimmed - use quotes and escapes where appropriate''', required: false, args: 2, valueSeparator: '=', argName: 'property=value')
OptionAccessor opt = cli.parse(args)

if(opt.h /*|| opt.arguments().isEmpty()*/ ) {
	cli.usage()
}
else {
	if(opt.D) {
		(opt.Ds as List).collate(2).each {// Override configs from commandline options
			// @TODO BUG?:
			//	--config "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
			// works while:
			//	'operations.printFolderSizes.treeTraverseOrder="breadthFirst"'
			// parsed into strings: it[0]=[operations.printFolderSizes.treeTraverseOrder], it[1]=["breadthFirst]
			GlobalConf.setFromPropertyPathLikeKey(it[0] as String, it[1]);
		}
	}
}
//println GlobalConf.log.test
GlobalConf.opt = opt;

//println "GlobalConf.log.test=" + GlobalConf.log.test

// use memcached to do not long await start and information gathering:
def mem = new MemcachedClientExtended(new InetSocketAddress('127.0.0.1', 11211));

ImapTreeSize tree1 = mem.getOrCreate('tree1', new ImapTreeTranscoder((ImapAccount)GlobalConf.accounts.Ant)) {
	new ImapTreeSize(GlobalConf.accounts.Ant)
};
ImapTreeSize tree2 = mem.getOrCreate('tree2', new ImapTreeTranscoder((ImapAccount)GlobalConf.accounts.PahanTest)) {
	new ImapTreeSize(GlobalConf.accounts.PahanTest)
};

FolderMessagesDiff foldersDiff = new FolderMessagesDiff(tree1.tree.@folder, tree2.tree.@folder);
foldersDiff.dump('Before copy missed messages');

FolderMessagesCopier copier = new FolderMessagesCopier(foldersDiff);
copier.copyMissedMessagesToFolder2();

new FolderMessagesDiff(foldersDiff).dump('After copy missed messages');

println 'Done'
// To do not hang
mem.shutdown();