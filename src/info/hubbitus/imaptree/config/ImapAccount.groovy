package info.hubbitus.imaptree.config

import groovy.transform.CompileStatic
import groovy.transform.TupleConstructor

/**
 * Config object ro represent main account entity
 *
 * @author Pavel Alexeev - <Pahan@Hubbitus.info> (pasha)
 * @created 2015-01-01 21:06
 **/
@TupleConstructor
@CompileStatic
class ImapAccount{
	// To refer it for example in log
	String name;

	String host = 'imap.gmail.com';
	Integer port = 933;
	String login;
	String password;
	// Google Imap assumed. You may try use generic 'imap' or 'imaps' instead of 'gimaps', it should work but have not tested. Please let me known if you need it but it does not work
	String type = 'gimaps';
//		folder = 'BAK_test' // Initial folder to scan. @TODO: implement pseudo folder like 'full account'
	String folder = 'INBOX';
}
