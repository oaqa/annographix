#!/bin/bash
mvn exec:java -Dexec.mainClass=org.apache.uima.examples.cpe.SimpleRunCPE  -Dexec.args=src/main/resources/CpeDescriptor.xml
