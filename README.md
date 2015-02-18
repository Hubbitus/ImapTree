ImapTree
========

Recursive walk by IMAP Folders and apply some `Operation`, default one - calculate Folder sizes (itself and subtree; in bytes, messages and subfolders). Written on Groovy and very customizible.

History of born you may read below in dedicated chapter. Firstly concept and futures.

# How to start

It written in Groovy language and use runtime dependencies, so run should bee simple as:
* Clone `repo` and go to `src` directory:

		git clone https://github.com/Hubbitus/ImapTree.git
		cd into ImapTree/src
* Configure. Default configuration is good enough for most use cases (but ma bee small excessive in logging). Provide account(s) access configuration is minimum what really required (like host, port, login and password). Good start will be to just copy provided [configuration/AccountsConfig-Example.groovy](https://github.com/Hubbitus/ImapTree/blob/master/src/configuration/AccountsConfig-Example.groovy) file with excessive commentaries:

		cp configuration/AccountsConfig-Example.groove configuration/AccountsConfig.groovy
* You are done! You may just run it:

		./ImapTree.groovy

## Usage information

By default it run to print folder sizes under defined root if you are define single only account.
Available command line option displayed by `-h` key:

		$ ./ImapTree.groovy -h
		usage: groovy
		 -a,--account <arg>             Account name from defined in config file under config.accounts section. Required if you define more than one there. Otherwise warning printed and its selected automatically.
		 -c,--cached                    run from cached file. No information gathered from imap account actually. Instead read previously saved file config.cache.xml.llXmlCache. Useful for deep analysis and experiments.
		 -d,--print-depth <arg>         Max amount of nesting folders print. By default all. Starts from 1, so only root folder will be printed. 2 means also 1st level childs and so on.
		 -D,--config <property=value>   Change configured options from command line. Allow runtime override. May appear multiple times - processed in that order. For example:
		                                -D cache.xml.fullXmlCache="some.file" --config operations.printFolderSizes.folderProcess='{true}' -D operations.printFolderSizes.messageProcess='{m-> println "SUBJ: ${m.subject}"}' --config
		                                "operations.printFolderSizes.treeTraverseOrder='breadthFirst'"
		                                Values trimmed - use quotes and escapes where appropriate
		 -h,--help                      This usage information
		 -o,--operation <arg>           Operation to perform. Otherwise just folder sizes printed (default operation named printFolderSizes).
		                                By default (in example config) implemented operations:
		                                o printFolderSizes - (default if no other defined) - just print on console sizes (in bytes, human readable size and count of messages and sub-folders)
		                                o eachMessage - print subject of each message. Good start to customise message processing.
		                                o GroovyConsole - Opens GUI GroovyConsole with binded gathered data and snippet to start from investigate it in interactive mode.
		                                o gmailTrueDeleteMessages - Real delete messages from Gmail-Imap to do not waste space (delete from '[Gmail]All mail'). Please see example config for detailed description problem and
		                                o fxTreeTable - GUI Tree, Table on JavaFX. I think it is the best instrument just for investigation like "where my space". Unfortunately there some problems and it may not work out of the box. Please read further description in Config-Example.groovy.
		                                solution
		                                See example config comments for more details.

## Example of run

		./ImapTreeSizes_Print.groovy -c --print-depth 2 --account Pahan
		Results should looks something like:
		Run from cache file [/home/pasha/Projects/ImapTreeSize/.results/Pahan.data.xml]
		<<BAK_test>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=5,50 GiB (5909473741), messages=173859}; treeChilds: 524
		<<BAK_test/Ant>>: SelfSize: {Size: bytes=0 B (0), messages=0}; treeSize: {Size: bytes=5,50 GiB (5909473741), messages=173859}; treeChilds: 523
		<<BAK_test/Ant/Archives>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=114,06 KiB (116796), messages=51}; treeChilds: 4
		<<BAK_test/Ant/Drafts>>: SelfSize: {Size: bytes=11,69 MiB (12260536), messages=24}; treeSize: {Size: bytes=11,69 MiB (12260536), messages=24}; treeChilds: 1
		<<BAK_test/Ant/GROUP_TALKS>>: SelfSize: {Size: bytes=107,24 MiB (112446479), messages=793}; treeSize: {Size: bytes=981,31 MiB (1028976940), messages=6185}; treeChilds: 128
		<<BAK_test/Ant/INBOX>>: SelfSize: {Size: bytes=36,96 MiB (38757376), messages=392}; treeSize: {Size: bytes=407,31 MiB (427098515), messages=30625}; treeChilds: 167
		<<BAK_test/Ant/INNER>>: SelfSize: {Size: bytes=7,02 MiB (7356846), messages=111}; treeSize: {Size: bytes=1,15 GiB (1238854162), messages=6015}; treeChilds: 101
		<<BAK_test/Ant/Junk>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
		<<BAK_test/Ant/Other>>: SelfSize: {Size: bytes=683,11 KiB (699500), messages=12}; treeSize: {Size: bytes=42,19 MiB (44239848), messages=233}; treeChilds: 8
		<<BAK_test/Ant/Sent>>: SelfSize: {Size: bytes=3,94 MiB (4126711), messages=51}; treeSize: {Size: bytes=3,94 MiB (4126711), messages=51}; treeChilds: 1
		<<BAK_test/Ant/Spam>>: SelfSize: {Size: bytes=293,12 KiB (300152), messages=2}; treeSize: {Size: bytes=293,12 KiB (300152), messages=2}; treeChilds: 1
		<<BAK_test/Ant/Templates>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
		<<BAK_test/Ant/Trash>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (0), messages=0}; treeChilds: 2
		<<BAK_test/Ant/_BUGS>>: SelfSize: {Size: bytes=261,28 MiB (273969544), messages=18565}; treeSize: {Size: bytes=1,93 GiB (2069954971), messages=118745}; treeChilds: 9
		<<BAK_test/Ant/disp_events>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=50,40 MiB (52850365), messages=771}; treeChilds: 15
		<<BAK_test/Ant/test>>: SelfSize: {Size: bytes=1,02 MiB (1064451), messages=118}; treeSize: {Size: bytes=1,02 MiB (1064451), messages=118}; treeChilds: 1
		<<BAK_test/Ant/Архивы>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
		<<BAK_test/Ant/ГТО>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=78,49 MiB (82306575), messages=137}; treeChilds: 13
		<<BAK_test/Ant/РГКs>>: SelfSize: {Size: bytes=88,10 KiB (90212), messages=6}; treeSize: {Size: bytes=903,44 MiB (947323719), messages=10902}; treeChilds: 68

## Concepts of options and operations

### Operations
It is introduced to be highly customisable and you may in minute get results in JSON, XML, XLS and many oter formats.
Main unit of data processing there is Tree of folders which is retrieved and cached (`-c`) and some kind of defined *`Operation`* on it.
In help before you may have been notified of list default implemented `operations`. Abstract `operation` is object instantiated [Operation](https://github.com/Hubbitus/ImapTree/blob/master/src/info/hubbitus/imaptree/config/Operation.groovy) class.
For very simple example:
```groovy
	simpleEachMessage = new Operation(
		folderProcess: {Node node->
			println "Folder ${node.name()}"
			true // Process messages also or not
		}
		,messageProcess: {Message m->
			println "Message with subject [${m.subject}] in folder <${m.folder}>"
		}
	)
```
You have there full pover of Groovy to manipulate folders of messages. Read below about implemented futures and usage directions.

`Operations` defined in config among other predefined.

_But what if you are want start you new Operation with different options? Slightly modify? Change parameter? Mix defined Closures (aka actions, processors, functions) from different Options?
Off course you may write arbitrary amount off operation many time copy&paste most of code…_
There flexible `configuration options` became very usable:

### Runtime configuration
With `-D/--config` option you may redefine on single run from command line near to all configuration! String, Integer, even Closure (aka actions, processors, functions)!
Look at breaf introduction. F.e. you may wish to do not change your config, but just change message processing letter and include in it also Gmail Labels:
Just add to run command option:

		$ ./ImapTree.groovy --account Backup --operation eachMessage --config operations.simpleEachMessage.messageProcess='{m-> println "(SUBJ: [${m.subject}]) Labels: ${m.getLabels()}"}'

It will be parsed, compiled and merged with you confgi from file on the fly.

Do not satisfied with defined hooks like `folderOpen/folderProcess/folderClose/messageProcess`? No problem, you may just define `fullControl` closure! Then single full object will bee passed into it and it up to you what you will do. In that fasion `GroovyConsole` `operation` defined for interactive investigation gathered data.

# Futures
* It simple, but powerfull.
* [ConfigSlurper and ConfigObject](http://groovy.codehaus.org/ConfigSlurper) syntax used for `configuration`, it is easy to read and maintain, allow define non-primitive types, referencing and even programming.
* Concept of `Operations` (described before) allow easy define almost any actions on Folders and/or messages. Sure you may use arbitraty filtering and aggregation.
* Several default `operations` predefined.
* Almost any in `config` defined option may be *rederined in commandline* via `-D/--config`. It give you vaste for experiments without needs to deal with files each time.
* By default `logging` enabled in console and also in 'process.log'. It may be customized in file [log4j2.xml](https://github.com/Hubbitus/ImapTree/blob/master/src/log4j2.xml). It is [well-documented apache log4j v.2 syntax](http://logging.apache.org/log4j/2.0/manual/index.html).
* Amount of Messages may be very big, so `progress logged` in user-frendly form, f.e.:

		main INFO  imaptree.ImapTreeSize - Process Node #1 from 523 (0,19%). Spent (pack 10 elements) time: 0,026 (from start: 0,027)
		main INFO  imaptree.ImapTreeSize - Process Node #10 from 523 (1,91%). Spent (pack 10 elements) time: 0,127 (from start: 0,278), Estimated items: 513, time: 14,280
* Fully `cached mode` to do not gather data from server each time (for folders, messages process always online, but connection auto restored).
* In process of investigation you may run `Operation` `GroovyConsole` and experiment with you data without even reread chache file!

## Implemented operations and interactive use with GUI

* printFolderSizes - (default if no other defined) - just print on console sizes (in bytes, human readable size and count of messages and sub-folders)
* eachMessage - print subject of each message. Good start to customise message processing.
* gmailTrueDeleteMessages - Real delete messages from Gmail-Imap to do not waste space (delete from '[Gmail]All mail'). Please see example config for detailed description problem and
* GroovyConsole - Opens GUI GroovyConsole with binded gathered data and snippet to start from investigate it in interactive mode.

		$ ./ImapTree.groovy --account BackupTest --operation GroovyConsole -c
![Screenshot of run *GroovyConsole* operations](https://raw.githubusercontent.com/Hubbitus/ImapTree/master/resources/screenshots/ImapTree-GroovyConsoleOperation.png)

* fxTreeTable - GUI Tree, Table on JavaFX. I think it is the best instrument just for investigation like "where my space". Unfortunately there some problems and it may not work out of the box. Please read further description in Config-Example.groovy.
![Screenshot of run *fxTreeTable* operation](https://raw.githubusercontent.com/Hubbitus/ImapTree/master/resources/screenshots/ImapTree-fxTreeTableOperation.png)

## Usage directions
* Firstly you may use it as described before for default operations to:
 * Print/export IMAP tree sizes (folders, messages)
 * Investigate deep in interactive mode.
 * Clean space on Gmail mail boxes
* Create reports for mailbox
* Drow charts
* Gather statistics
* Parse mails. F.e. filter mails by sender, take URLs and open brawser tabs for each.
* Suggestions?

### Hope it helps
Distributed under GPLv3+ Licens with hope it may somone helps but without any warranty.

Comments, future requests, bugs and any other feedback are very welcome!

## History introduction

It was born as simple script to calculate sizes of my Gmail Imap folder used as backup place for work mail from us very thin corporate account for historical purposes.

For mail sync great [offlineimap](http://offlineimap.org/) software used.

After several attempts to copy huge amount of mails (>170000) to my own google Apps mail account for history search I discover it also became almost full.

First naive attempts to reclaim space did fails. I have delete some folders with 10k mails, but free space did not graw.

I was must be solve 2 main task:

1. **Answer questions what happened and why**.
2. **Move backup folder into another (specially created and empty) account with reclaim free space there**.

## Where my space and how to understand who are eat it

Googling not so short time for ready solution fail. Rare solution seams abandoned and 1 for windows only which I do not use.

Then was first my attempt - run [imapsync] (http://imapsync.lamiral.info/) with same account as hack and option `--justfoldersizes`. Then write handler to decode UTF7, php-script to parse output into something readable for me and also acceptable for further processing… As hack it was work, but it is very-very hard to maintain and use.

In any case it lack amny desired futures and was not usefull.

## How to reclaim space??

GMail have special settings in options how to handle deleted via IMAP messages. But unfortunately it fully ignored - see file [devel/Delete.Tests](https://github.com/Hubbitus/ImapTree/blob/master/src/devel/Delete.Tests) (mostly Russian) for more details and experiments.
Handling IMAP actions also specific: https://support.google.com/mail/answer/77657?hl=en

Really it leave not so many possibilities:

1. Setup move messages to '[Gmail]/Trash' folder and delete messages *without* hold Shift key - then messages fully deleted from ALL folders if there was copies.
 1. It does not work with folders - localized labels like '[Gmail]/Корзина/My deleted folder' (in Russian) created instead.
2. If use Shift key or do not move messages into '[Gmail]/Trash' messages appeared in '[Gmail]/All mail' and still eat space.
3. Deleting label from Gmail web-interface also leave messages in '[Gmail]/All mail'.

So this `operation` `gmailTrueDeleteMessages` solve that problem for me and work in next way:

1. For configured root folder walk recursively and for each message:
2. If it present in more than just that folder - just remove from that folder (delete label).
3. If it does not present in any other folders - move it into '[Gmail]/Trash' (localized version should be handled correctly for all languages).

Essentially space from messages must be freed.


## Alternatives

Ehhh. It was found ta the end of developing. Developing on [Google Apps Script](https://developers.google.com/apps-script/overview) seams great because may interact witn mail inside Google services.
There also goode [case-studies examples](https://developers.google.com/apps-script/case-studies) to start learn how it works.
