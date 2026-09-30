#backup viewNode pos
python backupPos.py --uri mongodb://dci:dciworld%40iivi@localhost:27010/sotn?authSource=admin

#restore viewNode pos
python restorePos.py --uri mongodb://dci:dciworld%40iivi@localhost:27010/sotn?authSource=admin --csv nodes.csv
