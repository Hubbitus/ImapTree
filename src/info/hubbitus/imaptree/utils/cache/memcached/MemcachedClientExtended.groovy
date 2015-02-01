package info.hubbitus.imaptree.utils.cache.memcached

import groovy.transform.InheritConstructors
@Grab('net.spy:spymemcached:2.10.5')
import net.spy.memcached.MemcachedClient
import net.spy.memcached.transcoders.SerializingTranscoder
import net.spy.memcached.transcoders.Transcoder

/**
 * Add into MemcachedClient convenient method getOrCreate in groovy way.
 * See example in <a href="https://github.com/Hubbitus/groovy-test-examples/blob/master/Caching/Memcached.groovy">Memcached example</a>
 * of getOrCreate method in dynamic
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-28 00:45
 **/
@InheritConstructors
class MemcachedClientExtended extends MemcachedClient{
	static Map<Class, Transcoder> TRANSCODERS = [:];

	/**
	 * Return stored in cache key if it "groovy true" (so always evaluated if it null of false)? so cached, or create by
	 * provided closure, store and return result. It make use it more convenient like (assume
	 * 	def mem = new MemcachedClient( new InetSocketAddress('127.0.0.1', 11211 ) ) in all examples);
	 * Date d = mem.getOrCreate('testDate'){ new Date() }
	 *	instead of:
	 * Date d = mem.get('testDate');
	 * if (!d) d = new Date();
	 *
	 * Especially if calculation is more complex and can't be represent in one line (in that case elvis operator may be useful also):
	 * Date d = mem.get('testDate') ?: new Date();
	 *
	 * @param key
	 * @param secs
	 * @param create
	 * @return
	 */
	def getOrCreate(String key, Integer secs, Transcoder transcoder, Closure create){
		def res = get(key, transcoder);
		if (!res){
			res = create();
			set(key, secs, res, transcoder);
		}

		return res;
	}

	def getOrCreate(String key, Transcoder transcoder, Closure create) {
		getOrCreate(key, 3600, transcoder, create);
	}

	/**
	 * Variant of {@see getOrCreate} with default 3600 seconds store period
	 *
	 * @param key
	 * @param create
	 * @return
	 */
	def getOrCreate(String key, Closure create){
		getOrCreate(key, 3600, new SerializingTranscoder(), create);
	}
}
