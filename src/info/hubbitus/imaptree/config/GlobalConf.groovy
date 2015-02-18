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
 * It assume Config.groovy exists. That's all, it will be loaded and parsed on demand.
 *
 * In handled config "includeScript( OtherConfig )" functionality implemented
 *
 * @author Pavel Alexeev <Pahan@Hubbitus.info>
 * @created 15.07.2013 22:27 (ais), 16.07.2014 17:12:59 (rewrote rng), 03.02.2015 01:55:14 (imported and reworked)
 */
@Singleton
class GlobalConf {
	private static final String FILENAME = '/Config.groovy';

	/**
	 * For long run services it may be desired to auto-watch config change and read it on the fly
	 */
	private static final boolean USE_FILE_CHANGE_AUTO_WATCH = false;

	private ConfigExtended _conf;
	private Thread watcher;

	/**
	 * (Re)initialization_
	 */
	public void init(){
		if (USE_FILE_CHANGE_AUTO_WATCH) {
			// Start watching for file change. Unfortunately only watch dir implemented in Java 7, not just file change
			Path watchDir = Paths.get(this.getClass().getResource(FILENAME).toURI().resolve('.'));
			// https://weblogs.java.net/blog/kohsuke/archive/2007/04/how_to_convert.html, http://stackoverflow.com/questions/10159186/how-to-get-parent-url-in-java

			WatchService watchService = FileSystems.getDefault().newWatchService();
			watchDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

			// Processing will be by done watch service in another thread to in sane way handle also manual placed there files
			watcher = Thread.start('ConfigChangeWatchThread'){
				//noinspection GroovyInfiniteLoopStatement
				while(true) {
					WatchKey key = watchService.take();
					if(key) {
						key.pollEvents().each {
							if(it.context().toString().endsWith(FILENAME.substring(1))) {// Check without /
								onConfigChange();
							}
						}
						key.reset();
					}
					sleep(100); // 100 ms
				}
			}
		}
		onConfigChange();
	}

	/**
	 * For load config used workaround https://github.com/Hubbitus/groovy-test-examples/commit/38f521f64bb8999861537922317b61e83045b08e
	 * for implement "includeScript( OtherConfig )" functionality.
	 */
	synchronized private void onConfigChange(){
		_conf = (ConfigExtended)new ConfigSlurper().parse(this.getClass().getResource(FILENAME));
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
//		MetaProperty mp = GlobalConf.hasProperty(prop);
//		if (mp) return mp.getProperty(GlobalConf); // Real properties returned as is (class for example) to break loop

		if (!GlobalConf.getInstance()._conf) GlobalConf.getInstance().init(); // Not property, to break loop!
		GlobalConf.getInstance()._conf."$prop";
	}

	static void setFromPropertyPathLikeKey(String propertyLikeKey, value){
		if (!GlobalConf.getInstance()._conf) GlobalConf.getInstance().init(); // Not property, to break loop!
		GlobalConf.getInstance()._conf.setFromPropertyPathLikeKey(propertyLikeKey, value);
	}

	static void overrideFromListPropertiesPairs(/*List | false*/ options){
		if (!GlobalConf.getInstance()._conf) GlobalConf.getInstance().init(); // Not property, to break loop!
		GlobalConf.getInstance()._conf.overrideFromListPropertiesPairs(options);
	}
}
