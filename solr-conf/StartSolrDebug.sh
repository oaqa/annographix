#!/bin/bash
rm -f SolrStart.log
java  -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=1044  -server -Dsolr.solr.home=./indexes  -Dsolr.plugin.dir=./indexes/plugins -jar start.jar -Djava.util.logging.config.file=etc/logging.properties 2>&1|tee SolrStart.log 
