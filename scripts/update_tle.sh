#!/bin/bash

NAMES=(stations gps-ops glo-ops resource iridium intelsat iridium-NEXT globalstar orbcomm ses musson science)
FOLDER=~
TLEFILE=../src/main/resources/sats.tle

# Delete the file
> $TLEFILE

# Load in each Satellite Data
for a in "${NAMES[@]}"
do
	echo $a
	url="curl -s -o ${FOLDER}/${a}.txt https://celestrak.org/NORAD/elements/gp.php?GROUP=${a}&FORMAT=tle"
	$(${url})
	cat ${FOLDER}/${a}.txt >> $TLEFILE
done


