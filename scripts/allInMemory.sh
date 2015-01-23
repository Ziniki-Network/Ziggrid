#!/bin/sh

java -cp dist/lib/ZiggridMemoryDemo.jar org.ziggrid.main.Ziggrid --model SampleData/buckets ziggrid-baseball --storage memory --generator file:Generator/src/main/resources/baseball.xml --limit 100000 --observer --threads 0 1 1 --web --static BaseballDemo
