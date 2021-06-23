#!/bin/sh 
set -ex 


find ~/.ivy2/local/me.lightspeed7/ -name "*$1*" -print | xargs rm -fr

sbt ';cc ;+publishLocalSigned'
