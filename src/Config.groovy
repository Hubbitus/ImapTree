import configuration.AccountsConfig
import configuration.logging.FolderMessagesDiffLoggerFilesLogtrait
import configuration.operations.*
import info.hubbitus.imaptree.config.ComposedConfigScript

//@Grab(group = 'org.codehaus.groovyfx', module = 'groovyfx', version = '0.3.1')
/**
 * Look {@see ComposedConfigScript} for info why it is not just script
 */
class MainConfig extends ComposedConfigScript {
	def run() { // normal contents of a config file go in here
		includeScript( AccountsConfig )

		/**
		 * Operations to perform.
		 * One default operation defined historically - just print folder sizes,
		 * @TODO May be it hawe worh just load all from some folder?
		 */
		operations {
			includeScript( PrintFolderSizeOperation )
			includeScript( EachMessageOperation )
			includeScript( GroovyConsoleOperation )
			includeScript( GmailTrueDeleteMessagesOperation )
			includeScript( FxTreeTableOperation )
		}

		cache {
			xml {
				// To save full cache of results to operate next time from it. Provide false there to disable writing
				// %{account} will be replaced by used account name
				fullXmlCache = ".results/%{account}.data.xml"
			}

			/**
			 * To use it you should install, configure and run memcached server. As it very fast it is very useful for several
			 * subsequent experiments (debug) with assumption what underlined data has not be changed.
			 * NOTE - no automatic invalidation happened, so enable it use, disable and refill fully on you choose!
			 * If it enabled by --memcached and there present actual (not expired) information for that name of account it will be used. Otherwise gathering on IMAP performed and such cache filled.
			 * You may want use <a href="http://www.alphadevx.com/a/90-Accessing-Memcached-from-the-command-line">command line client</a> for example.
			 */
			memcached {
				host = '127.0.0.1'
				port = 11211
				/**
				 * Seconds on what tree stored. After that it will be purged
				 */
				cachetime = 3600
			}
		}

		log {
			/**
			 * Enable JavaMail logging of IMAP for these accounts. List of account names (by name field provided, not
			 * config path)
			 * By default empty list
			 */
			imapdebug = ['PahanTest']

			diff {
				includeScript( FolderMessagesDiffLoggerFilesLogtrait )
			}
		}
	}
}