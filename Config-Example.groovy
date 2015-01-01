// By default Config.groovy used as configuration! So, rename this file first, or adjust path in example scripts accordingly!!!
config{
	account{
		host = 'imap.gmail.com'
		port = 933
		login = 'some.name@gmail.com'
		password = 'Super-Pass'
		// Google Imap assumed. You may try use generic 'imap' or 'imaps' instead of 'gimaps', it should work but have not tested. Please let me known if you need it but it does not work
		type = 'gimaps'
		folder = 'INBOX' // Initial folder to scan. @TODO: implement pseudo folder like 'full account'
	}
}