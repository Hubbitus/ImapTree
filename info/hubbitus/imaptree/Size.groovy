package info.hubbitus.imaptree

import groovy.transform.AutoClone
import groovy.transform.Sortable

/**
 * Class to represent ImapFolder size
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info>
 * @created 2015-01-01 17:49
 **/
@Sortable
@AutoClone
class Size {
	Long bytes = 0 // Size in bytes of messages in folder
	Long messages = 0// Number of messages in folder
	boolean onlyFolders = false

	// http://stackoverflow.com/questions/3758606/how-to-convert-byte-size-into-human-readable-format-in-java
	public static String humanReadableByteCount(long bytes, boolean si = false) {
		int unit = si ? 1000 : 1024;
		if(bytes < unit) return bytes + " B";
		int exp = (int) (Math.log(bytes) / Math.log(unit));
		String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1).toString() + (si ? "" : "i");
		return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
	}

	String hr(){
		humanReadableByteCount(bytes ?: 0)
	}

	Size plus(Size other) {
		Size ret = this.clone()

		if(null == ret.bytes) ret.bytes = 0;
		if(null == ret.messages) ret.messages = 0;
		ret.bytes += other.bytes ?: 0;
		ret.messages += other.messages ?: 0;
		return ret;
	}

	String toString() {
		"{Size: bytes=${hr()} (${bytes}), messages=${messages}}"
	}
}