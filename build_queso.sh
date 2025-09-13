#!/bin/bash

rm SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar
cd SymbolicOptimizer
mvn package -Dmaven.test.skip
cd ..
mv SymbolicOptimizer/target/SymbolicOptimizer-1.0-SNAPSHOT-jar-with-dependencies.jar .