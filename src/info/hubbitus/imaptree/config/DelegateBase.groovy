package info.hubbitus.imaptree.config

/**
 * Base class for copy closure-based nesting objects constructor
 * In all nested childs added constructor (Map map) which follow standard groovy map-based syntax set fields, but
 * additionally for all closures from map:
 * 1 change delegate to this and
 * 2 change resolveStrategy to Closure.DELEGATE_ONLY;
 *
 * WARNING: Unfortunately not enough annotate child classes as @InheritConstructors
 * because in this base constructor no child fields. So in most cases explicit constructor:
 * Child(Map map){ ConstructFromMap(map); }
 * should be added.
 *
 * @author Pavel Alexeev <Pahan@Hubbitus.info>
 * @created 2014-09-17 20:45 Born in ais adapter
 * @imported 2015.01.06 04:05:27
 **/
class DelegateBase{
	/**
	 * Constructor to allow groovy map syntax and additionally init resulting metaClass also
	 * {@see <a href="http://stackoverflow.com/a/6268928/307525">http://stackoverflow.com/a/6268928/307525}</a>}
	 *
	 * We MUST declare 0 argument default constructor and this with map to allow standard groovy Map-like object creation
	 * but also closure delegate triggering
	 *
	 * @param map
	 */
	DelegateBase(Map map){
		ConstructFromMap(map);
	}

	/**
	 * Method to construct from childs constructors. See further notes in class docs.
	 * @param map
	 */
	public ConstructFromMap(Map map){
		for (entry in map) {
			redelegateClosure(entry.key, entry.value);
		}
	}

	DelegateBase(){}

	public void setProperty(String name, value){
		redelegateClosure(name, value);
	}

	/**
	 * Safe closure redelegate to This object. If value is not Closure - just set it.
	 * See notes for class
	 *
	 * @param name
	 * @param value
	 */
	protected void redelegateClosure(String name, value){
		if (value instanceof Closure){
			value.delegate = this;
			value.resolveStrategy = Closure.DELEGATE_ONLY;
		}

		if(this.respondsTo((String)"set${name.with{ it[0].toUpperCase() + it.substring(1) }}")){
			this."set${name.with{ it[0].toUpperCase() + it.substring(1) }}"(value);
		}
		else{
			this.@"$name" = value;
		}
	}
}