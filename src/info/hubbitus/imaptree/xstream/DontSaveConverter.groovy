package info.hubbitus.imaptree.xstream

import com.thoughtworks.xstream.converters.SingleValueConverter
import groovy.transform.CompileStatic

/**
 * Simple Xstream converter to omit some thing from store into XML and just reconstruct their on the fly in provided
 * value on read file and reconstruct objects phase
 *
 * @author Pavel Alexeev <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-05 22:39:29
 */
@CompileStatic
public class DontSaveConverter implements SingleValueConverter {
	private Class type;
	private def outerValueOnRestore;

	DontSaveConverter(Class type, outerValueOnRestore){
		this.type = type
		this.outerValueOnRestore = outerValueOnRestore
	}

	@Override
	boolean canConvert(Class type){
		return type in this.type;
	}

	@Override
	String toString(Object obj){
		return '_not_saved_'
	}

	@Override
	Object fromString(String str){
		return outerValueOnRestore;
	}
}