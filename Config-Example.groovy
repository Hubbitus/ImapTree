import info.hubbitus.imaptree.ImapAccount

// By default Config.groovy used as configuration! So, rename this file first, or adjust path in example scripts accordingly!!!
config{
	// You may define different accounts to choose later which use like:
	// ImapTreeSize imapTree = new ImapTreeSize(config.account1);
	// ImapTreeSize imapTree = new ImapTreeSize(config.account2);
	account1 = new ImapAccount(
		host: 'imap.gmail.com'
		,port: 933
		,login: 'some.name@gmail.com'
		,password: 'Super-Pass'
		// Google Imap assumed. You may try use generic 'imap' or 'imaps' instead of 'gimaps', it should work but have not tested. Please let me known if you need it but it does not work
		,type: 'gimaps'
		,folder: 'INBOX' // Initial folder to scan. @TODO: implement pseudo folder like 'full account'
	)
	// If you are good with defaults, you may use short form:
	account2 = new ImapAccount(
		login: 'some.name2@gmail.com'
		,password: 'Super-Pass for name 2'
	)
	// Some sort ov nesting available also with groovy ConfigSlurper magick:
	account3 = account1.with{
		login = 'some.name3@gmail.com'
		password = 'Super-Pass for name 3'
	}
	// and also constructor style form available (example same as account1):
	account4 = new ImapAccount('imap.gmail.com', 933, 'some.name@gmail.com', 'Super-Pass', 'gimaps', 'INBOX')
}