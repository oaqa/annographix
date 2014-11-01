This directory contains only configs, but no binaries. You can copy configs to an example Solr installation directory. You can use the script StartSolr.sh to start Solr. It also creates a simple script StopSolr.sh that can be used to kill the Solr instance (note that there are better ways to stop/start Solr).

###port###
-------------------

The port is configured to be 8984. Should you need to change it, update the following files:

```
SolrConfig/etc/jetty.xml:      <Set name="port"><SystemProperty name="jetty.port" default="8984"/></Set>
SolrConfig/etc/jetty.xml:      java -Djavax.net.ssl.trustStore=../etc/solrtest.keystore -Durl=https://localhost:8984/solr/update -jar post.jar *.xml
SolrConfig/etc/jetty.xml:      java -Djavax.net.ssl.keyStorePassword=secret -Djavax.net.ssl.keyStore=../etc/solrtest.keystore -Djavax.net.ssl.trustStore=../etc/solrtest.keystore -Durl=https://localhost:8984/solr/update -jar post.jar *.xml
SolrIndexScripts/post.sh:      URL=http://localhost:8984/solr/annographix/update
```

###instance name###
-------------------
The name of the instance is **annographix**. To change the name, grep config files for the occurrences of this name. You also need to change the name of the instance directory (a subdirectory of the directory indexes).

###jar placement###
-------------------
The SOLR extension jar needs to be placed into the **lib** subdirectory of **each** core/instance directory. In this example, it should be in indexes/annographix/lib. In theory, you should be able to specify an arbitrary location in a config file, but these feature seems to be broken:

https://issues.apache.org/jira/browse/SOLR-5708


To obtain a jar file, just got to the code directory and type:
```
mvn package
```
and retrieve a jar from the subdirectory **target**. 
 
###stemmer configuration###
-------------------
The provided template schema is configured to use Hunspell for stemming. After installing Hunspell, one should go the directory **indexes/annographix/conf** and create links to Hunspell files. If the installion directory is /usr/share/hunspell one can type the following:
```
ln -s /usr/share/hunspell/en_US.aff
ln -s /usr/share/hunspell/en_US.dic
```
Alternatively, one can disable Hunspell stemmer in the config and/or use any other stemmer instead.

###storing the values text fields###
-------------------
The template configs indicates that text fields should be stored (i.e., their original values should be memorized). This requires extra storage, but allows one to easily rename field names:

http://searchivarius.org/blog/how-rename-fields-solr

To save space, set the value of the attribute **stored** to false.
