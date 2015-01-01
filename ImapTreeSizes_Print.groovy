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
cli.c(longOpt: 'cached', 'run from cached file. No information gathered from imap account actually. Instead read previously saved file config.fullXmlCache. Useful for deep analysis and experiments.', required: false)
OptionAccessor opt = cli.parse(args)

if(opt.h /* || opt.arguments().isEmpty() */ ) {
	cli.usage()
}
else{
	ImapTreeSize imapTree;

	if (!opt.c){
		imapTree = new ImapTreeSize(config.account);
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

	imapTree.tree.depthFirst().each{
		println "<<${it.name()}>>: Size: ${it.@size}"
	}
}

//		Map res = imapTree.tree.depthFirst().collect { n ->
//			[
//					name        : n.name()
//					, selfSize  : n.@size
//					, treeSize  : n.depthFirst().sum { it.@size }
//					, treeChilds: n.depthFirst().size()
//			]
//		}

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