package configuration.operations

import info.hubbitus.imaptree.config.Operation

import javax.mail.Message

eachMessage = new Operation(
	folderProcess: {Node node->
		println "eachMessage process folder <<${node.name()}>>: SelfSize: ${node.@size}; subTreeSize: ${node.depthFirst().sum { it.@size }}; childSubtreeFolders: ${node.depthFirst().size() - 1}"
		true
	}
	,messageProcess: {Message m->
		println "<<${m.folder}>> (Folder attributes: ${m.folder.getAttributes()})); (Labels: ${m.getLabels()}); {SUBJ: ${m.subject}}"
	}
)