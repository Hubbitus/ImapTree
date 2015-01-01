#!/opt/groovy-2.3.6/bin/groovy

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
OptionAccessor opt = cli.parse(args)

if(opt.h /*|| opt.arguments().isEmpty()*/ ) {
	cli.usage()
}
else{
	ImapTreeSize imapTree;

	if (!opt.c){
		switch(true){
			case !config.accounts:
				throw new RuntimeException('To run not in cached (-c) mode you must configure at least one parameter in config-file Config.groovy. See Config-Example.groovy for details');

			case config.accounts.size() > 1 && !opt.a:
				throw new RuntimeException ('To run not in cached (-c) mode you must provide desired account name in commandline via -a/--account option');

			default:
				println ("You do not provide account as option, but only one defined in config. [${config.accounts[opt.a]}] will be used.");
		}

		imapTree = new ImapTreeSize(config.accounts[opt.a]);
		if (config.log.fullXmlCache){
			File file = new File(config.log.fullXmlCache);
			if(file.exists()) file.delete();
			file << imapTree.serializeToXML();
		}
	}
	else{
		println('Run from cache');
		imapTree = ImapTreeSize.fromCacheXMLfile(new File(config.log.fullXmlCache));
	}

//	Node.metaClass.depthLevel = {
//		delegate.folder.name().split(
//			(it.folder.value() instanceof NodeList ? it.folder.value()[0] : it.folder.value()).separator.toString()
//		).size()
//		77
//	}

// First very simple implementation, without subtrees
//	imapTree.tree.depthFirst().each{
//		println "<<${it.name()}>>: Size: ${it.@size}"
//	}

	List<Map> res = imapTree.tree.depthFirst().collect{Node n ->
		[
			node       : n
			,selfSize  : n.@size
			,treeSize  : n.depthFirst().sum { it.@size }
			,treeChilds: n.depthFirst().size()
		]
	};

	res.each{
//		println("it.node.name(): ${it.node.name()}")
//		println("it.node.@folder: ${it.node.@folder}")
//		println("Depth: ${it.node.name().split(it.node.@folder.separator.toString()).size()}")
		if (opt.'print-depth' && (it.node.name().split(it.node.@folder.separator.toString()).size() <= opt.'print-depth'.toInteger()) ){
			println "<<${it.node.name()}>>: SelfSize: ${it.selfSize}; treeSize: ${it.treeSize}; treeChilds: ${it.treeChilds}"
		}
	}
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