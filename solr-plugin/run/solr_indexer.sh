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
textField=$4
if [ "$textField" = "" ] ; then
  echo "Specify textField (4th arg)"
  exit 1
fi
annotField=$5
if [ "$annotField" = "" ] ; then
  echo "Specify annotField (5th arg)"
  exit 1
fi
batchQty=""
if [ "$6" != "" ] ; then
  batchQty=" -n $6 "
fi
mvn compile exec:java -Dexec.mainClass=edu.cmu.lti.oaqa.annographix.apps.SolrIndexApp  -Dexec.args="-t $text -a $annot -u $uri -textField $textField -annotField $annotField $batchQty"
