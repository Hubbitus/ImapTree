package configuration.operations

import info.hubbitus.imaptree.config.GlobalConf
import info.hubbitus.imaptree.config.Operation

import javax.mail.Message

printFolderSizes = new Operation(
	folderProcess: {Node node->
		if ( (false == GlobalConf.opt.'print-depth') || (node.name().split(node.@folder.separator.toString()).size() <= GlobalConf.opt.'print-depth'.toInteger()) ){
			println "<<${node.name()}>>: SelfSize: ${node.@size}; subTreeSize: ${node.depthFirst().sum { it.@size }}; childSubtreeFolders: ${node.depthFirst().size() - 1}"
		}
		false
	}
	,messageProcess: {Message m-> } // Is not used in particular case (false returned from folder handler), but for example may contain: println m.subject
)