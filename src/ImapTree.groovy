#!/opt/groovy-2.3.6/bin/groovy
import info.hubbitus.imaptree.config.CliBuilderAutoWidth
import info.hubbitus.imaptree.config.ImapAccount
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.Operation
import info.hubbitus.imaptree.utils.ConfigExtended

@Grab(group='commons-cli', module='commons-cli', version='1.2')

/**
 * Simple example to just print folder sizes recursively
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-01 19:22
 * */


ConfigExtended config = (ConfigExtended)new ConfigSlurper().parse(Config).config;

CliBuilderAutoWidth cli = new CliBuilderAutoWidth(/*usage: 'Usage:'*/)
cli.h(longOpt: 'help', 'This usage information', required: false)
cli.a(longOpt: 'account', 'Account name from defined in config file under config.accounts section. Required if you define more than one there. Otherwise warning printed and its selected automatically.', required: false, args: 1)
cli.c(longOpt: 'cached', 'run from cached file. No information gathered from imap account actually. Instead read previously saved file config.fullXmlCache. Useful for deep analysis and experiments.', required: false)
cli.d(longOpt: 'print-depth', 'Max amount of nesting folders print. By default all. Starts from 1, so only root folder will be printed. 2 means also 1st level childs and so on.', required: false, args: 1)
cli.o(longOpt: 'operation', '''Operation to perform. Otherwise just folder sizes printed (default operation named printFolderSizes).
By default (in example config) implemented operations:
	o printFolderSizes - (default if no other defined) - just print on console sizes (in bytes, human readable size and count of messages and sub-folders)
	o eachMessage - print subject of each message. Good start to customise message processing.
	o GroovyConsole - Opens GUI GroovyConsole with binded gathered data and snippet to start from investigate it in interactive mode.
	o gmailTrueDeleteMessages - Real delete messages from Gmail-Imap to do not waste space (delete from '[Gmail]All mail'). Please see example config for detailed description problem and solution
	o fxTreeTable - GUI Tree, Table on JavaFX. I think it is the best instrument just for investigation like "where my space". Unfortunately there some problems and it may not work out of the box. Please read further description in Config-Example.groovy.
See example config comments for more details.''', required: false, args: 1)
cli.D(longOpt: 'config', '''Change configured options from command line. Allow runtime override. May appear multiple times - processed in that order. For example:
	-D log.fullXmlCache="some.file" --config operations.printFolderSizes.folderProcess='{true}' -D operations.printFolderSizes.messageProcess='{m-> println "SUBJ: ${m.subject}"}' --config "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
	Values trimmed - use quotes and escapes where appropriate''', required: false, args: 2, valueSeparator: '=', argName: 'property=value')
OptionAccessor opt = cli.parse(args)

config.opt = opt;

if(opt.h /*|| opt.arguments().isEmpty()*/ ) {
	cli.usage()
}
else{
	if (opt.D){
		(opt.Ds as List).collate(2).each{// Override configs from commandline options
// @TODO BUG?:
//			--config "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
// works while:
//			'operations.printFolderSizes.treeTraverseOrder="breadthFirst"'
// parsed into strings: it[0]=[operations.printFolderSizes.treeTraverseOrder], it[1]=["breadthFirst]
//			println "it[0]=[${it[0]}], it[1]=[${it[1]}]"
			config.setFromPropertyPathLikeKey(it[0] as String, it[1]);
		}
	}

	ImapTreeSize imapTree;
	String accountName;
	ImapAccount imapAccount;

	switch(true){
		case !config.accounts:
			throw new RuntimeException('You must configure at least one parameter in config-file Config.groovy. See Config-Example.groovy for details');

		case config.accounts.size() > 1 && !opt.a:
			throw new RuntimeException ('You must provide desired account name in commandline via -a/--account option because multiple accounts defined in config!');

		case 1 == config.accounts.size():
			accountName = config.accounts.find{true}.key;
			imapAccount = config.accounts.find{true}.value;
			println ("You do not provide account as option, but only one defined in config. [${accountName}] will be used.");
			break;

		default:
			accountName = opt.a;
			imapAccount = (ImapAccount)config.accounts[accountName];
			if (!imapAccount){
				throw new RuntimeException ("It seams requested account [$accountName] is not defined in config!");
			}
	}
	File xmlCacheFile = new File((String)config.log.fullXmlCache.replace('%{account}', accountName));

	if (!opt.c){
		imapTree = new ImapTreeSize(imapAccount);
		if (config.log.fullXmlCache){
			imapTree.serializeToFile(xmlCacheFile);
		}
	}
	else{
		println("Run from cache file [${xmlCacheFile.absolutePath}]");
		imapTree = ImapTreeSize.deserialize(xmlCacheFile, imapAccount);
	}

	Operation operation = (opt.operation ? config.operations[opt.operation] : config.operations.printFolderSizes) as Operation;
	operation.config = config;
		if (operation.fullControl){
			operation.fullControl(imapTree);
		}else{
			imapTree.traverseTree(operation);
		}
}
