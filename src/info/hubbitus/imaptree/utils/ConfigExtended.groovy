package info.hubbitus.imaptree.utils

/**
 * Main goal to add functionality to ConfigObject GDK class
 *
 * First it allow operations opposite flatten, set hierarchy from string, like:
 * ConfigExtended conf = …
 *	conf.setFromPropertyPathLikeKey('some.deep.hierarchy.of.properties', value)
 * and then access it as usual:
 * conf.some.deep.hierarchy.of.properties
 * not as it is one string property.
 * conf.'some.deep.hierarchy.of.properties'
 *
 * Additionally it override merge of ConfigObjects and do not replace completely replace Objects but set properties of it.
 * For example:
 * Standard behaviour:
 * // Uncomment next line if you are plan run example from GroovyConsole to handle defined there classes: http://groovy.329449.n5.nabble.com/GroovyConsole-and-context-thread-loader-td4471707.html
 * // Thread.currentThread().contextClassLoader = getClass().classLoader
 * @groovy.transform.ToString
 * class Test{
 * 	String s = 's initial'
 * 	Integer i = 77
 * }
 *
 * ConfigObject config = new ConfigSlurper().parse('''config{
 * 	some.property = 'value'
 * 	test = new Test()
 * }''').config
 *
 * ConfigObject config1 = new ConfigSlurper().parse('''config{ test.s = 's change' }''').config
 *
 * config.merge(config1)
 * assert config.test == 's change'
 *
 * BUT stop, why config.test replaced? Our intention was to set only their field s!
 * That class do that
 *
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-03 22:10
 **/
class ConfigExtended extends ConfigObject{
	/**
	 * It is not work set it from doted string, but read in groovy syntax like:
	 * ConfigObject conf = new ConfigObject();
	 * conf.'some.key' = 77;
	 * assert conf.some.key == [:]
	 *
	 * It is also safe for keys without dots.
	 *
	 * WARNING! Because this method so powerful (f.e. it give ability provide closures definition from command line),
	 * there no easy possibility distinguish intentions provide atoms or string literals! So when someValue passed it
	 * is it variable name or string value? So, strings must be quoted in regular syntax, so call may look like:
	 * config.setFromPropertyPathLikeKey('some.test.s', '"s CHANGED"');
	 *
	 * Born in ais adapter.
	 *
	 * @param conf
	 * @param propertyLikeKey
	 * @param value
	 */
	public void setFromPropertyPathLikeKey(String propertyLikeKey, value){
		merge((ConfigObject)new ConfigSlurper().parse( "config{ $propertyLikeKey = $value }" ).config);
	}

	/**
	 * Override merge and doMerge to allow save Objects other than ConfigObject and just set theirs properties (fields).
	 * See more details and example in class description
	 *
	 * @param other The ConfigObject to merge with
	 * @return The result of the merge
	 */
	@Override
	public Map merge(ConfigObject other) {
		return doMerge(this, other);
	}

	/**
	 * Override merge and doMerge to allow save Objects other than ConfigObject and just set theirs properties (fields).
	 * See more details and example in class description
	 *
	 * @param config
	 * @param other
	 * @return
	 */
	private Map doMerge(Map config, Map other) {
		for (Object o : other.entrySet()) {
			Map.Entry next = (Map.Entry) o;
			Object key = next.getKey();
			Object value = next.getValue();

			Object configEntry = config.get(key);

			if (configEntry == null) {
				config.put(key, value);
			} else {
				if (configEntry instanceof Map && ((Map)configEntry).size() > 0 && value instanceof Map) {
					// recur
					doMerge((Map) configEntry, (Map) value);
				} else {
					if (configEntry instanceof Map){ // As parent
						config.put(key, value);
					}
					else{ // Addition to do not replace object by instead modify its properties inplace
						value.flatten().each{prop->
							configEntry."${prop.key}" = prop.value;
						}
					}
				}
			}
		}

		return config;
	}

	/**
	 * Special method for CliBuilder options like configuration partially redefine like:
	 * def cli = new CliBuilderAutoWidth()
	 *	cli.D(longOpt: 'config', '''Change configured options from command line. Allow runtime override. May appear multiple times - processed in that order. For example:
	 *	-D log.fullXmlCache="some.file" --config operations.printFolderSizes.folderProcess='{true}' -D operations.printFolderSizes.messageProcess='{m-> println "SUBJ: ${m.subject}"}' --config "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
	 *	Values trimmed - use quotes and escapes where appropriate''', required: false, args: 2, valueSeparator: '=', argName: 'property=value')
	 *	if(opt.D) {
	 *		(opt.Ds as List).collate(2).each {// Override configs from commandline options
	 *			GlobalConf.setFromPropertyPathLikeKey(it[0] as String, it[1]);
	 *		}
	 *	}
	 *became just:
	 * config.overrideFromListPropertiesPairs(opt.Ds as List)
	 * or even simple:
	 * GlobalConf.overrideFromListPropertiesPairs(opt.Ds as List)
	 *
	 * @param options
	 */
	void overrideFromListPropertiesPairs(List options){
		options.collate(2).each {// Override configs from commandline options
			// @TODO BUG?:
			//	--config "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
			// works while:
			//	'operations.printFolderSizes.treeTraverseOrder="breadthFirst"'
			// parsed into strings: it[0]=[operations.printFolderSizes.treeTraverseOrder], it[1]=["breadthFirst]
			setFromPropertyPathLikeKey(it[0] as String, it[1]);
		}
	}
}
