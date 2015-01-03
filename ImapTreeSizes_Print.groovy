#!/opt/groovy-2.3.6/bin/groovy
import info.hubbitus.imaptree.ImapAccount
import info.hubbitus.imaptree.ImapTreeSize

/**
 * Simple example to just print folder sizes recursively
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-01 19:22
 * */


ConfigObject config = new ConfigSlurper().parse(Config).config;

def cli = new CliBuilder(usage: 'Usage options: -[hc]')
cli.h(longOpt: 'help', 'usage information', required: false)
cli.a(longOpt: 'account', 'Account name from defined in config file under config.accounts section. Required if you define more than one there. Otherwise warning printed and its selected automatically.', required: false, args: 1)
cli.c(longOpt: 'cached', 'run from cached file. No information gathered from imap account actually. Instead read previously saved file config.fullXmlCache. Useful for deep analysis and experiments.', required: false)
cli._(longOpt: 'print-depth', 'Max amount of nesting folders print. By default all. Starts from 1, so only root folder will be printed. 2 means also 1st level childs and so on.', required: false, args: 1)
cli.o(longOpt: 'operation', 'Operation to perform. Otherwise just folder sizes printed (default operation named printFolderSizes)', required: false, args: 1)
OptionAccessor opt = cli.parse(args)

config.opt = opt;

if(opt.h /*|| opt.arguments().isEmpty()*/ ) {
	cli.usage()
}
else{
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
	File xmlCacheFile = new File(config.log.fullXmlCache.replace('%{account}', accountName));

	if (!opt.c){
		imapTree = new ImapTreeSize(config.accounts[opt.a]);
		if (config.log.fullXmlCache){
			imapTree.serializeToFile(xmlCacheFile);
		}
	}
	else{
		println("Run from cache file [${xmlCacheFile.absolutePath}]");
		imapTree = ImapTreeSize.deserializeFromFile(xmlCacheFile);
	}

	def operation = (opt.operation ? config.operations[opt.operation] : config.operations.printFolderSizes);
	imapTree.traverseTree(operation.folder, operation.message, config.operations.treeTraverseOrder)
}

/*
//import java.awt.*
import java.awt.FlowLayout
import java.awt.GridLayout
import javax.swing.*
import groovy.swing.SwingBuilder


aFrame = new SwingBuilder().frame(title:"Hello World",size:[200,200]){
    panel(layout: new FlowLayout()) {
      scrollPane(preferredSize:[200,130]) {
        tree(){
            node('one')
        }
      }
      panel(layout:new GridLayout(1,2,15,15)){
         button(text:"Ok")
         button(text:"Cancel")
      }
    }
}
aFrame.show()
*/