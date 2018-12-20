#!/bin/bash

NAMES=(stations gps-ops glo-ops resource iridium intelsat iridium-NEXT globalstar orbcomm ses musson science)
FOLDER=~
#TLEFILE=~/github/websats/src/main/resources/sats.tle
TLEFILE=../src/main/resources/sats.tle

#FOLDER=/home/tomcat
#TLEFILE=/opt/tomcat/webapps/websats/WEB-INF/classes/sats.tle

# Delete the file
> $TLEFILE

# Load in each Satellite Data
for a in "${NAMES[@]}"
do
	echo $a
	url="curl -s -o ${FOLDER}/${a}.txt https://www.celestrak.com/NORAD/elements/${a}.txt"
	$(${url})
	cat ${FOLDER}/${a}.txt >> $TLEFILE
	
done

#/home/david/tomcat8/bin/shutdown.sh 
#/home/david/tomcat8/bin/startup.sh

#systemctl stop tomcat8.service
#systemctl start tomcat8.service


