#!/bin/bash

NAMES=(stations gps-ops glo-ops resource iridium intelsat iridium-NEXT globalstar orbcomm ses musson science)
FOLDER=/home/tomcat
TLEFILE=/opt/tomcat/webapps/websats/WEB-INF/classes/sats.tle

> $TLEFILE

for a in "${NAMES[@]}"
do
	url="curl -s -o ${FOLDER}/${a}.txt https://www.celestrak.com/NORAD/elements/${a}.txt"
	$(${url})
	cat ${FOLDER}/${a}.txt >> $TLEFILE
	
done

systemctl stop tomcat8.service
systemctl start tomcat8.service


