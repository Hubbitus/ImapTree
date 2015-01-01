#!/opt/groovy-2.3.6/bin/groovy

import info.hubbitus.imaptree.ImapTreeSize

/**
 * Simple example to just print folder sizes recursively
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-01 19:22
 * */


ConfigObject config = new ConfigSlurper().parse(Config).config;

ImapTreeSize imapTree = new ImapTreeSize(config.account);

imapTree.tree.depthFirst().each{
	println "<<${it.name()}>>: Size: ${it.@size}"
}

/*Map res = imapTree.tree.depthFirst().collect { n ->
	[
		name        : n.name()
		, selfSize  : n.@size
		, treeSize  : n.depthFirst().sum { it.@size }
		, treeChilds: n.depthFirst().size()
	]
}

XStream xstream = new XStream(
	new XppDriver() { // For console run
		public HierarchicalStreamWriter createWriter(Writer out) {
			return new PrettyPrintWriter(out) {
				protected void writeText(QuickWriter writer, String text) {
					if(text ==~ /(?s).*[<>&].*//*) {
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

OutputStream stream = new ByteArrayOutputStream();
xstream.toXML(res)

File file = new File('res.xml');
if(file.exists()) file.delete();
file << xstream.toXML(res);*/

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