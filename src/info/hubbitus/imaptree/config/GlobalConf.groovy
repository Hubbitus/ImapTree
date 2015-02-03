package info.hubbitus.imaptree.config

import info.hubbitus.imaptree.utils.ConfigExtended

import java.nio.file.*

/**
 * Simple config holder for sharing. Singleton with global scope.
 *
 * We all known old Singleton pattern, and off course it main purpose in nowadays limited to internal usage in libraries
 * als most should agree…
 * But stop, in groovy scripts together with ConfigObject and ConfigSlurper  it resurrected to best global helper - it
 * so amazing to in each place just wrote:
 *	GlobalConf.field ?: …
 *	Of GlobalConf.someAction(…)
 *
 * Off course it may be desired use DI and IOC for some big systems, but it just easy and better use Singleton for
 * scripts as I thought.
 *
 * It assume (convention) Config.groovy exists and it present configuration in main config tag like: "config { }"
 * That's all, it will b loaded and parsed on demand.
 *
 * @author Pavel Alexeev <Pahan@Hubbitus.info>
 * @created 15.07.2013 22:27 (ais), 16.07.2014 17:12:59 (rewrote rng), 03.02.2015 01:55:14 (imported and reworked)
 */
@Singleton
class GlobalConf {
	private static final String FILENAME = '/Config.groovy';

	private ConfigExtended _conf;
	private Thread watcher;

	/**
	 * (Re)initialization_
	 */
	public void init(){
		// Start watching for file change. Unfortunately only watch dir implemented in Java 7, not just file change
		Path watchDir = Paths.get(this.getClass().getResource(FILENAME).toURI().resolve('.')); // https://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html, http://stackoverflow.com/questions/10159186/how-to-get-parent-url-in-java

		WatchService watchService = FileSystems.getDefault().newWatchService();
		watchDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

		// Processing will be by done watch service in another thread to in sane way handle also manual placed there files
		watcher = Thread.start('ConfigChangeWatchThread'){
			//noinspection GroovyInfiniteLoopStatement
			while(true){
				WatchKey key = watchService.take();
				if (key) {
					key.pollEvents().each{
						if (it.context().toString().endsWith(FILENAME.substring(1))){// Check without /
							onConfigChange();
						}
					}
					key.reset();
				}
				sleep(100); // 100 ms
			}
		}
		onConfigChange();
	}

	synchronized private void onConfigChange(){
		_conf = (ConfigExtended)new ConfigSlurper().parse(this.getClass().getResource(FILENAME)).config;
	}

	/**
	 * {@see http://jira.codehaus.org/browse/GROOVY-6264#comment-328878}
	 *
	 * @param name
	 * @param value
	 * @return
	 */
	public static void set(String name, value){
		if (!GlobalConf.instance._conf) GlobalConf.instance.init();
		GlobalConf.instance._conf."$name" = value;
	}

	/**
	 * Alternative to Config.metaClass.static.propertyMissing (which is not always working on some reason?) way
	 * https://groups.google.com/forum/#!topic/groovy-user/3tCGPYDT3fQ
	 *
	 * @param prop
	 * @return
	 */
	public static def get(String prop){
		MetaProperty mp = GlobalConf.hasProperty(prop);
		if (mp) return mp.getProperty(GlobalConf); // Real properties returned as is (class for example) to break loop

		if (!GlobalConf.getInstance()._conf) GlobalConf.getInstance().init(); // Not property, to break loop!
		GlobalConf.getInstance()._conf."$prop";
	}

	/**
	 * Proxy also methods call to _conf if so, for example {@see info.hubbitus.imaptree.utils.ConfigExtended#setFromPropertyPathLikeKey(java.lang.String, java.lang.Object)}
	 *
	 * @param name
	 * @param args
	 * @return
	 */
//	static{ // For static methods it just little harder than methodMissing - http://groovy.codehaus.org/ExpandoMetaClass+-+Overriding+static+invokeMethod
//		GlobalConf.metaClass.static.invokeMethod = {String name, args->
//			def metaMethod = delegate.class.metaClass.getStaticMetaMethod(name, args);
//			if(metaMethod) return metaMethod.invoke(delegate, args);
//			else{
//				if (!GlobalConf.instance._conf) GlobalConf.instance.init(); // Not property, to break loop!
//				if (GlobalConf.instance._conf.respondsTo(name)){
//					GlobalConf.instance._conf."$name"(*args);
//				}
//				else throw new MissingMethodException(name, ConfigExtended, args)
//			}
//		}
//	}

	static void setFromPropertyPathLikeKey(String propertyLikeKey, value){
		if (!GlobalConf.getInstance()._conf) GlobalConf.getInstance().init(); // Not property, to break loop!
		GlobalConf.getInstance()._conf.setFromPropertyPathLikeKey(propertyLikeKey, value);
	}
}
