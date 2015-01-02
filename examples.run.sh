#!/bin/bash

# For example you copy some folder from one account to another and then want compare results.
# First obtain stat information from two accounts:
./ImapTreeSizes_Print.groovy --account Pahan
./ImapTreeSizes_Print.groovy --account Backup

# And then analize results
./ImapTreeSizes_Print.groovy -c --print-depth 2 --account Pahan
# Results should looks something like:
# Run from cache file [/home/pasha/Projects/ImapTreeSize/.results/Pahan.data.xml]
# <<BAK_test>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=5,50 GiB (5909473741), messages=173859}; treeChilds: 524
# <<BAK_test/Ant>>: SelfSize: {Size: bytes=0 B (0), messages=0}; treeSize: {Size: bytes=5,50 GiB (5909473741), messages=173859}; treeChilds: 523
# <<BAK_test/Ant/Archives>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=114,06 KiB (116796), messages=51}; treeChilds: 4
# <<BAK_test/Ant/Drafts>>: SelfSize: {Size: bytes=11,69 MiB (12260536), messages=24}; treeSize: {Size: bytes=11,69 MiB (12260536), messages=24}; treeChilds: 1
# <<BAK_test/Ant/GROUP_TALKS>>: SelfSize: {Size: bytes=107,24 MiB (112446479), messages=793}; treeSize: {Size: bytes=981,31 MiB (1028976940), messages=6185}; treeChilds: 128
# <<BAK_test/Ant/INBOX>>: SelfSize: {Size: bytes=36,96 MiB (38757376), messages=392}; treeSize: {Size: bytes=407,31 MiB (427098515), messages=30625}; treeChilds: 167
# <<BAK_test/Ant/INNER>>: SelfSize: {Size: bytes=7,02 MiB (7356846), messages=111}; treeSize: {Size: bytes=1,15 GiB (1238854162), messages=6015}; treeChilds: 101
# <<BAK_test/Ant/Junk>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
# <<BAK_test/Ant/Other>>: SelfSize: {Size: bytes=683,11 KiB (699500), messages=12}; treeSize: {Size: bytes=42,19 MiB (44239848), messages=233}; treeChilds: 8
# <<BAK_test/Ant/Sent>>: SelfSize: {Size: bytes=3,94 MiB (4126711), messages=51}; treeSize: {Size: bytes=3,94 MiB (4126711), messages=51}; treeChilds: 1
# <<BAK_test/Ant/Spam>>: SelfSize: {Size: bytes=293,12 KiB (300152), messages=2}; treeSize: {Size: bytes=293,12 KiB (300152), messages=2}; treeChilds: 1
# <<BAK_test/Ant/Templates>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
# <<BAK_test/Ant/Trash>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (0), messages=0}; treeChilds: 2
# <<BAK_test/Ant/_BUGS>>: SelfSize: {Size: bytes=261,28 MiB (273969544), messages=18565}; treeSize: {Size: bytes=1,93 GiB (2069954971), messages=118745}; treeChilds: 9
# <<BAK_test/Ant/disp_events>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=50,40 MiB (52850365), messages=771}; treeChilds: 15
# <<BAK_test/Ant/test>>: SelfSize: {Size: bytes=1,02 MiB (1064451), messages=118}; treeSize: {Size: bytes=1,02 MiB (1064451), messages=118}; treeChilds: 1
# <<BAK_test/Ant/Архивы>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
# <<BAK_test/Ant/ГТО>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=78,49 MiB (82306575), messages=137}; treeChilds: 13
# <<BAK_test/Ant/РГКs>>: SelfSize: {Size: bytes=88,10 KiB (90212), messages=6}; treeSize: {Size: bytes=903,44 MiB (947323719), messages=10902}; treeChilds: 68

./ImapTreeSizes_Print.groovy -c --print-depth 2 --account Backup
# Results should looks something like:
# <<BAK>>: SelfSize: {Size: bytes=0 B (0), messages=0}; treeSize: {Size: bytes=5,72 GiB (6136555324), messages=172413}; treeChilds: 523
# <<BAK/Ant>>: SelfSize: {Size: bytes=0 B (0), messages=0}; treeSize: {Size: bytes=5,72 GiB (6136555324), messages=172413}; treeChilds: 522
# <<BAK/Ant/Archives>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=178,51 KiB (182798), messages=93}; treeChilds: 4
# <<BAK/Ant/Drafts>>: SelfSize: {Size: bytes=11,69 MiB (12260536), messages=24}; treeSize: {Size: bytes=11,69 MiB (12260536), messages=24}; treeChilds: 1
# <<BAK/Ant/GROUP_TALKS>>: SelfSize: {Size: bytes=107,56 MiB (112784368), messages=802}; treeSize: {Size: bytes=994,56 MiB (1042874467), messages=6248}; treeChilds: 128
# <<BAK/Ant/INBOX>>: SelfSize: {Size: bytes=37,62 MiB (39449978), messages=417}; treeSize: {Size: bytes=400,43 MiB (419876074), messages=25752}; treeChilds: 166
# <<BAK/Ant/INNER>>: SelfSize: {Size: bytes=7,03 MiB (7375397), messages=113}; treeSize: {Size: bytes=1,19 GiB (1276720840), messages=6139}; treeChilds: 101
# <<BAK/Ant/Junk>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
# <<BAK/Ant/Other>>: SelfSize: {Size: bytes=683,11 KiB (699500), messages=12}; treeSize: {Size: bytes=42,19 MiB (44239848), messages=233}; treeChilds: 8
# <<BAK/Ant/Sent>>: SelfSize: {Size: bytes=3,94 MiB (4126711), messages=51}; treeSize: {Size: bytes=3,94 MiB (4126711), messages=51}; treeChilds: 1
# <<BAK/Ant/Spam>>: SelfSize: {Size: bytes=293,12 KiB (300152), messages=2}; treeSize: {Size: bytes=293,12 KiB (300152), messages=2}; treeChilds: 1
# <<BAK/Ant/Templates>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
# <<BAK/Ant/Trash>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (0), messages=0}; treeChilds: 2
# <<BAK/Ant/_BUGS>>: SelfSize: {Size: bytes=261,50 MiB (274203979), messages=18574}; treeSize: {Size: bytes=2,01 GiB (2158256915), messages=120058}; treeChilds: 9
# <<BAK/Ant/disp_events>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=50,40 MiB (52852710), messages=773}; treeChilds: 15
# <<BAK/Ant/test>>: SelfSize: {Size: bytes=1,02 MiB (1064451), messages=118}; treeSize: {Size: bytes=1,02 MiB (1064451), messages=118}; treeChilds: 1
# <<BAK/Ant/Архивы>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=0 B (null), messages=0}; treeChilds: 1
# <<BAK/Ant/ГТО>>: SelfSize: {Size: bytes=0 B (null), messages=0}; treeSize: {Size: bytes=90,62 MiB (95017120), messages=148}; treeChilds: 13
# <<BAK/Ant/РГКs>>: SelfSize: {Size: bytes=528,59 KiB (541272), messages=36}; treeSize: {Size: bytes=981,12 MiB (1028782702), messages=12774}; treeChilds: 68