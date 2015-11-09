#!/bin/bash
set -e
## Script to pull, build, and redeploy a DSpace


site="OSU Mockup"
configDir="/dspace/config"
sourceDir="/dspace-source/osulibrariesDSpace"
tomcatStop="sudo /etc/init.d/tomcat6 stop"
tomcatStart="sudo /etc/init.d/tomcat6 start"

echo "Rebuild and Redeploy $site"

cd $configDir
if [[ -n $(git status --porcelain) ]]; then
   echo "Instance config Git repository is dirty, aborting. PWD: $PWD"
   exit 1
fi

echo "Instance config Git Repository is clean, continuing"


cd $sourceDir
if [[ -n $(git status --porcelain) ]]; then
   echo "DSpace-Source Git repository is dirty, aborting. PWD: $PWD"
   exit 1
fi

echo "DSpace-Source Git Repository is clean, continuing"



cd $sourceDir
git pull
mvn clean package
cd dspace/target/dspace-installer/

$tomcatStop
sleep 10


ant -Dconfig=$configDir/dspace.cfg update

cd $configDir
git reset HEAD --hard

$tomcatStart
