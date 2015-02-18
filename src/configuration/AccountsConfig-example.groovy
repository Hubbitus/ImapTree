package configuration

import info.hubbitus.imaptree.config.ImapAccount

// By default Config.groovy used as configuration! So, rename this file first, or adjust path in example scripts accordingly!!!
accounts {
	// You may define different accounts to choose later which use like:
	// ImapTreeSize imapTree = new ImapTreeSize(config.account1);
	// ImapTreeSize imapTree = new ImapTreeSize(config.account2);
	// In runtime you will be then choose it by -a/--account option like: --account account1
	account1 = new ImapAccount(
		name: 'FirstAccount'
		, host: 'imap.gmail.com'
		, port: 933
		, login: 'some.name@gmail.com'
		, password: 'Super-Pass'
		// Google Imap assumed. You may try use generic 'imap' or 'imaps' instead of 'gimaps', it should work but have not tested. Please let me known if you need it but it does not work
		, type: 'gimaps'
		, folder: 'INBOX' // Initial folder to scan.
	)
	// If you are good with defaults, you may use short form:
	account2 = new ImapAccount(
		name: 'For backup'
		, login: 'some.name2@gmail.com'
		// To read password from file for example.
		, password: 'cat .passfile'.execute().text
	)
	// Some sort ov nesting available also with groovy ConfigSlurper magic:
	account3 = account1.with {
		name = 'Account like 1st'
		login = 'some.name3@gmail.com'
		password = 'Super-Pass for name 3'
	}
	// and also constructor style form available (example same as account1):
	account4 = new ImapAccount('4th', 'imap.gmail.com', 933, 'some.name@gmail.com', 'Super-Pass', 'gimaps', 'INBOX')
}