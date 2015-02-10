package info.hubbitus.imaptree.utils.errors

import groovy.util.logging.Log4j2

/**
 * Trait to add capability evaluate user provided closure in сруслув manner: Catch exception and write it in log with name
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info>
 * @created 08-02-2015 7:31 PM
 **/
@Log4j2
trait EvaluateClosureChecked{
	static def evaluateClosureChecked(String name, Closure what){
		try{
			what()
		}
		catch(Throwable t){
			log.error("Evaluating [$name] failed: ", t);
		}
	}

	/**
	 * Should be applied (possible at runtime) to ConfigObject or similar behaved object. Then it became possible auto
	 * check provided closures evaluation.
	 *
	 * @TODO Can't be implemented in that way due to the groovy bug: https://jira.codehaus.org/browse/GROOVY-7295
	 *
	 * @param confPath
	 * @param args
	 * @return
	 */
	def evaluateClosureChecked(String confPath, ...args){
		try{
			this.flatten()[confPath](*args);
		}
		catch(Throwable t){
			log.error("Evaluating [$confPath] failed: ", t);
		}
	}
}
