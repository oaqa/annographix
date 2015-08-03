#/bin/bash
text=$1
if [ "$text" = "" ] ; then
  echo "Specify input file (1st arg)"
  exit 1
fi
uri=$2
if [ "$uri" = "" ] ; then
  echo "Specify uri (2d arg)"
  exit 1
fi
batchQty=""
if [ "$3" != "" ] ; then
  batchQty=" -n $3 "
fi
echo "Input: '$text' Uri: '$uri' batchQty: '$batchQty'"
mvn compile exec:java -Dexec.mainClass=edu.cmu.lti.oaqa.annographix.apps.SolrSimpleIndexApp  -Dexec.args=" -i $text -u $uri $batchQty"
