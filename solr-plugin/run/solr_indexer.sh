#/bin/bash
text=$1
if [ "$text" = "" ] ; then
  echo "Specify text file (1st arg)"
  exit 1
fi
annot=$2
if [ "$annot" = "" ] ; then
  echo "Specify annot file (2d arg)"
  exit 1
fi
uri=$3
if [ "$uri" = "" ] ; then
  echo "Specify uri (3d arg)"
  exit 1
fi
batchQty=""
if [ "$4" != "" ] ; then
  batchQty=" -n $4 "
fi
mvn compile exec:java -Dexec.mainClass=edu.cmu.lti.oaqa.annographix.apps.SolrIndexApp  -Dexec.args="-t $text -a $annot -u $uri $batchQty"
