#!/bin/sh

[ -d tmp ] || mkdir tmp
find src -type f -name '*.java' -print0 | xargs -0 javac -d tmp
cd src
find . -type f -iname '*.js' -print0 | xargs -0 cp -t ../tmp --parents
cd ..
echo 'Main-Class: polyplot.PolyPlot' > manifest.txt
jar cfm polyplot.jar manifest.txt -C tmp .
rm -r manifest.txt tmp
