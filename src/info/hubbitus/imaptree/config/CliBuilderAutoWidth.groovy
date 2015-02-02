package info.hubbitus.imaptree.config

/**
 * Unix-hack to use full terminal width (by suggestion from http://stackoverflow.com/questions/1286461/can-i-find-the-console-width-with-java)
 * It was implemented in similar fashion (https://issues.apache.org/jira/browse/CLI-166), but then reverted because can't
 * be used for all systems in java-way
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-02-02 04:03
 * */
class CliBuilderAutoWidth extends CliBuilder{
	CliBuilderAutoWidth() {
		super();
		try{
			width = ["bash", "-c", "tput cols 2> /dev/tty"].execute().text.toInteger()
		}
		catch(IOException ignore){}
	}
}
