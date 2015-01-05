package info.hubbitus.imaptree.config

import javax.mail.Folder

/**
 * Class to traverse ImapTree folders and messages
 *
 * If you want full custom control you just provide fullControl closure and build all logic yourself.
 *
 * But in most cases it much easy provide only desired event handlers. All operations roughly process tree, so it may be
 * presented in meta-language something like (omit errors handling, logging and so on):
 *
 * tree.eachFolderInDesiredOrder{Folder f->
 *     if (folderProcess(f)){
 *         folderOpen(f)
 *         f.messages.each{Message m->
 *             messageOpen(m)
 *             messageProcess(m)
 *             messageClose(m)
 *         }
 *     }
 *     folderClose(f)
 * }
 *
 * Really folderProcess is required, otherwise depends on your needs.
 * Also Node passed instead of Folder, which holds more information.
 *
 * Return values treated as:
 *	true - process messages in it also
 *	false - otherwise
 *
 *
 * In closures body full this config available as config variable and it additionally have
 *	config.opt - CliBuilder parsed command line passed options.
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-05 00:33
 **/
//@AutoClone Simple annotation can't be used due to the bug: https://jira.codehaus.org/browse/GROOVY-7091 (fixed in groovy >= 2.3.8)
class Operation{
	// If it defined ALL OTHER IGNORED - for custom processors
	Closure fullControl;

	/**
	 * Will be injected actual config. It also provide config.opt - access to parsed commandline options
	 */
	ConfigObject config;

	// Name of method {@link groovy.util.Node} (http://groovy.codehaus.org/api/groovy/util/Node.html) for order tree traversal
	// breadthFirst or depthFirst. See <a href="http://en.wikipedia.org/wiki/Tree_traversal">theory</a>
	String treeTraverseOrder = 'depthFirst'

	int folderOpenMode = Folder.READ_ONLY;

	/**
	 * Handle folder - print, calculate stats, gather additional information and so on.
	 */
	Closure<Boolean> folderProcess = {
		if (config.opt.'print-depth' && (node.name().split(node.@folder.separator.toString()).size() <= config.opt.'print-depth'.toInteger()) ){
			println "<<${node.name()}>>: SelfSize: ${node.@size}; treeSize: ${node.depthFirst().sum { it.@size }}; treeChilds: ${node.depthFirst().size()}"
		}
		false
	}
	/**
	 * Main purpose open folder. Also handle restore from cache and initialisation
	 *
	 * Myst be not null if folderProcess returns true;
	 *
	 * @param f Node passed have next content:
	 *	value - (obtained by value()) is Folder if it leaf or NodeList otherwise
	 * Attributes:
	 *	folder - Folder
	 *	size - calculated size
	 *	root - boolean is flag is it root folder or not
	 */
	Closure folderOpen = {Node n->
		if(!((Folder) n.@folder).open){ // Allow open and initialize manually in folder processing closure
			// In case working from cache - init store and logger fields. Also no checking or results needed -it have been done in meta-getters
			n.@folder.store; n.@folder.logger;

			n.@folder.open(folderOpenMode);
		}
	}
	/**
	 * Called after process messages in folder. F.e. To close, expunge, calculate statistics if it changed
	 */
	Closure folderClose;

	Closure messageProcess;

	/**
	 * @AutoClone Simple annotation can't be used due to the bug: https://jira.codehaus.org/browse/GROOVY-7091 (fixed in groovy >= 2.3.8)
	 *
	 * @return
	 */
	@Override
	Operation clone(){
		Operation res = new Operation();
		this.properties.findAll{ 'class' != it.key }.each{
			res."${it.key}" = it.value
		}
		res;
	}
}
