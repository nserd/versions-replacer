#!/bin/bash

function check-paths {
    ! [ -f "$arg1" ] && echo "File $arg1 not found." && exit 1
    ! [ -f "$arg2" ] && echo "File $arg2 not found." && exit 1
}

function move-files-in-tmp-dir {
    fileName1=`basename $arg1`
    fileName2=`basename $arg2`
    tempDir="/tmp/temp_`date +%s`"

    mkdir $tempDir
    mv $arg1 $tempDir/$fileName1
    mv $arg2 $tempDir/$fileName2
}

function move-yaml-file-back {
    mv $tempDir/$fileName2 $arg2
    rm -rf $tempDir
}

arg1="$1"
arg2="$2"

[ `sudo docker images -q versions-replacer | wc -l` -eq 0 ] && sudo docker build -t versions-replacer .

if [ -z "$arg1" ]; then sudo docker run --rm versions-replacer; exit; fi
if [ -z "$arg2" ]; then sudo docker run --rm versions-replacer $arg1; exit; fi

check-paths
move-files-in-tmp-dir
sudo docker run -v $tempDir:/files --rm versions-replacer /files/$fileName1 /files/$fileName2
move-yaml-file-back