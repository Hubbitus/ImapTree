package info.hubbitus.imaptree.utils.cache.memcached

import groovy.transform.TupleConstructor
import info.hubbitus.imaptree.ImapTreeSize
import info.hubbitus.imaptree.config.ImapAccount
import net.spy.memcached.CachedData
import net.spy.memcached.transcoders.SerializingTranscoder
import net.spy.memcached.transcoders.Transcoder

/**
 *
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-29 00:02
 **/
@TupleConstructor
class ImapTreeTranscoder implements Transcoder{
	ImapAccount account;

	@Override
	boolean asyncDecode(CachedData d){
		return false
	}

	@Override
	CachedData encode(Object o){
		String str = ((ImapTreeSize)o).serialize();
		return new CachedData(0, str.bytes, str.size());
	}

	@Override
	Object decode(CachedData d){
		return ImapTreeSize.deserialize(new String(d.data), account);
	}

	@Override
	int getMaxSize(){
		return CachedData.MAX_SIZE;
	}
}
