#!/bin/bash

java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar EnumeratorPrune -g ibm -q 3 -s 4
java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar EnumeratorPrune -g nam -q 3 -s 6
java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar EnumeratorPrune -g rigetti -q 3 -s 5
java --enable-preview -cp SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar EnumeratorPrune -g ion -q 3 -s 3