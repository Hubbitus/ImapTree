package info.hubbitus.imaptree.config

/**
 * Idea and base implementation from http://naleid.com/blog/2009/07/30/modularizing-groovy-config-files-with-a-dash-of-meta-programming
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info>
 * @created 18-02-2015 1:06 AM
 * */
public abstract class ComposedConfigScript extends Script {
	def includeScript(scriptClass) {
		try{
			def scriptInstance = scriptClass.newInstance()
			scriptInstance.metaClass = this.metaClass
			scriptInstance.binding = new ConfigBinding(this.getBinding().callable)
			scriptInstance.&run.call()
		}
		catch(Exception e){
			throw new RuntimeException("Can't includeScript [$scriptClass] for configuration: ", e);
		}
	}
}
