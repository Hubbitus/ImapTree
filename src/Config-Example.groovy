import info.hubbitus.imaptree.ImapAccount

import javax.mail.Message

// By default Config.groovy used as configuration! So, rename this file first, or adjust path in example scripts accordingly!!!
config{
	accounts{
		// You may define different accounts to choose later which use like:
		// ImapTreeSize imapTree = new ImapTreeSize(config.account1);
		// ImapTreeSize imapTree = new ImapTreeSize(config.account2);
		// In runtime you will be then choose it by -a/--account option like: --account account1
		account1 = new ImapAccount(
			host: 'imap.gmail.com'
			,port: 933
			,login: 'some.name@gmail.com'
			,password: 'Super-Pass'
			// Google Imap assumed. You may try use generic 'imap' or 'imaps' instead of 'gimaps', it should work but have not tested. Please let me known if you need it but it does not work
			,type: 'gimaps'
			,folder: 'INBOX' // Initial folder to scan.
		)
		// If you are good with defaults, you may use short form:
		account2 = new ImapAccount(
			login: 'some.name2@gmail.com'
			,password: 'Super-Pass for name 2'
		)
		// Some sort ov nesting available also with groovy ConfigSlurper magic:
		account3 = account1.with{
			login = 'some.name3@gmail.com'
			password = 'Super-Pass for name 3'
		}
		// and also constructor style form available (example same as account1):
		account4 = new ImapAccount('imap.gmail.com', 933, 'some.name@gmail.com', 'Super-Pass', 'gimaps', 'INBOX')
	}

	/**
	 * Operations to perform.
	 * One default operation defined historically - just print folder sizes,
	 *
	 */
	operations{
		// Name of method {@link groovy.util.Node} (http://groovy.codehaus.org/api/groovy/util/Node.html) for order tree traversal
		// breadthFirst or depthFirst. See <a href="http://en.wikipedia.org/wiki/Tree_traversal">theory</a>
		treeTraverseOrder = 'depthFirst'

		// Base example and default operation!
		printFolderSizes{
			/**
			 * Return values treated as:
			 *	true - process messages in it also
			 *	false - otherwise
			 *
			 * Node passed have next content:
			 *	value - (obtained by value()) is Folder if it leaf or NodeList otherwise
			 * Attributes:
			 *	@folder - Folder
			 *	@size - calculated size
			 *	@root - boolean is flag is it root folder or not
			 *
			 * In closures body full this config available as config variable and it additionally have
			 *	config.opt - CliBuilder parsed command line passed options.
			 */
			folder = {Node node->
				if (config.opt.'print-depth' && (node.name().split(node.@folder.separator.toString()).size() <= config.opt.'print-depth'.toInteger()) ){
					println "<<${node.name()}>>: SelfSize: ${node.@size}; treeSize: ${node.depthFirst().sum { it.@size }}; treeChilds: ${node.depthFirst().size()}"
				}
				false
			}
			message = {Message m-> } // Is not used in particular case (false returned from folder handler), but for example may contain: println m.subject
		}
	}

	log{
		// To save full cache of results to operate next time from it. Provide false there to disable writing
		// When read from file occurred no any validation or invalidation performed (IMAP may change over time).
		// Directory must exists
		// %{account} will be replaced by used account name
		fullXmlCache = '.results/%{account}.data.xml'
	}
}