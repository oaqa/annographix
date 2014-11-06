#!/bin/bash
uri=$1
if [ "$uri" = "" ] ; then
  echo "Specify uri (1st arg)"
  exit 1
fi

../solr-apps/run/solr_indexer.sh output/text.txt.gz output/annot_offsets.tsv.gz  $uri text annot 100
