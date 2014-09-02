#!/bin/bash

set -e

if [[ $# -eq 0 ]] ; then
  echo "Be sure to specify which environment build you want, i.e. dev, staging, prod."
  exit 0
fi

echo "You have specified input of [$1]"


cd /dspace-source/osulibrariesDSpace

if [ $1 = "staging" ]; then
  #Clean up from our previous staging cheating
  git checkout -- dspace/config/input-forms.xml
  git checkout -- dspace/config/xmlui.xconf
fi

##Ensure git is up to date?
git pull

if [ $1 = "staging" ]; then
  echo "This script is running on the staging server, so we will cheat the configs for input-form and xmlui."
  cp dspace/config/input-forms.staging.xml dspace/config/input-forms.xml
  cp dspace/config/xmlui.staging.xconf dspace/config/xmlui.xconf
fi

mvn -Denv=$1 package -DskipTests=true

cd dspace/target/dspace-1.8.3-SNAPSHOT-build/
ant update

sudo /etc/init.d/tomcat6 restart
