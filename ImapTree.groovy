#!/opt/groovy-2.3.6/bin/groovy
import info.hubbitus.imaptree.ImapAccount
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.utils.ConfigExtended

/**
 * Simple example to just print folder sizes recursively
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-01 19:22
 * */


ConfigExtended config = (ConfigExtended)new ConfigSlurper().parse(Config).config;

def cli = new CliBuilder(/*usage: 'Usage:'*/)
cli.h(longOpt: 'help', 'This usage information', required: false)
cli.a(longOpt: 'account', 'Account name from defined in config file under config.accounts section. Required if you define more than one there. Otherwise warning printed and its selected automatically.', required: false, args: 1)
cli.c(longOpt: 'cached', 'run from cached file. No information gathered from imap account actually. Instead read previously saved file config.fullXmlCache. Useful for deep analysis and experiments.', required: false)
cli.d(longOpt: 'print-depth', 'Max amount of nesting folders print. By default all. Starts from 1, so only root folder will be printed. 2 means also 1st level childs and so on.', required: false, args: 1)
cli.o(longOpt: 'operation', 'Operation to perform. Otherwise just folder sizes printed (default operation named printFolderSizes)', required: false, args: 1)
cli.D(longOpt: 'config', '''Change configured options from command line. Allow runtime override. May appear multiple times - processed in that order. For example:
	-D log.fullXmlCache="some.file" --config operations.printFolderSizes.folder='{true}' -D operations.printFolderSizes.message='{m-> println "SUBJ: ${m.subject}"}'
	Values trimmed - use quotes and escapes where appropriate''', required: false, args: 2, valueSeparator: '=', argName: 'property=value')
OptionAccessor opt = cli.parse(args)

config.opt = opt;

if(opt.h /*|| opt.arguments().isEmpty()*/ ) {
	/*
	* Unix-hack to use full terminal width (by suggestion from http://stackoverflow.com/questions/1286461/can-i-find-the-console-width-with-java)
	* It was implemented in similar fashion (https://issues.apache.org/jira/browse/CLI-166), but then reverted because can't
	* be used for all systems in java-way
	*/
	try{
		cli.width = ["bash", "-c", "tput cols 2> /dev/tty"].execute().text.toInteger()
	}
	catch(IOException ignore){}
	cli.usage()
}
else{
	if (opt.D){
		(opt.Ds as List).collate(2).each{// Override configs from commandline options
			config.setFromPropertyPathLikeKey(it[0], it[1]);
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