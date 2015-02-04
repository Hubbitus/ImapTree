package info.hubbitus.imaptree.utils.errors

import org.codehaus.groovy.runtime.StackTraceUtils

/**
 * Class to handle common tasks process errors
 *
 * @author Pavel Alexeev <Pahan@Hubbitus.info> (pasha)
 * @created 2014-01-29 19:08
 * @imported 02.02.2015 02:18:47 from ais adapter partially
 */
class ErrorsProcessing{
	/**
	 * Return caller method name as String
	 * Borrowed http://stackoverflow.com/questions/9540678/groovy-get-enclosing-functions-name
	 *
	 * @param stackDepth 1 return previous direct caller, but also may request any other
	 * @return String
	 */
	public static String callerMethodName(int stackDepth = 1){
		StackTraceUtils.sanitize(new Throwable()).stackTrace[2 + stackDepth].methodName
	}
}