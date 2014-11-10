#/bin/bash
queryFile=$1
if [ "$queryFile" = "" ] ; then
  echo "Specify query file (1st arg)"
  exit 1
fi
uri=$2
if [ "$uri" = "" ] ; then
  echo "Specify uri (2d arg)"
  exit 1
fi
trec_out=$3
if [ "$trec_out" = "" ] ; then
  echo "Specify the trec-style  output file (3d arg)"
  exit 1
fi
numRes=1000
mvn compile exec:java -Dexec.mainClass=edu.cmu.lti.oaqa.annographix.apps.SolrQueryApp  -Dexec.args="-q $queryFile -u $uri -n $numRes -o $trec_out"
