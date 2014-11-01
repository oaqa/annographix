#!/bin/bash
rm -f SolrStart.log
java -server -Dsolr.solr.home=./indexes -jar start.jar -Djava.util.logging.config.file=etc/logging.properties 2>>SolrStart.log 1>>SolrStart.log &
pid=$!
echo "echo \"Trying to kill $pid\" ; kill $pid ; kill $pid  ; kill $pid ; ps aux|grep $pid" > "StopSolr.sh"
chmod +x StopSolr.sh 
