#!/bin/bash
hubname="hub01"
echo -n "What is the hub name (${hubname}): "
read INP
if [ ! -z "${INP}" ]; then
DOCKER_TAG=${INP}
fi

satname="sat01"
echo -n "What is the sat name (${satname}): "
read INP
if [ ! -z "${INP}" ]; then
DOCKER_TAG=${INP}
fi

topic="websats-in"
echo -n "What is the sit name (${topic}): "
read INP
if [ ! -z "${INP}" ]; then
DOCKER_TAG=${INP}
fi

datasetname="websats"
echo -n "What is the datasetname (${datasetname}): "
read INP
if [ ! -z "${INP}" ]; then
DOCKER_TAG=${INP}
fi

echo "Creating getcounts.sh"

hubip=$(curl -s http://m1:8080/v2/apps/${hubname} | jq .app.tasks[0].ipAddresses[0].ipAddress | tr -d '"')
hubport=$(curl -s http://m1:8080/v2/apps/${hubname} | jq .app.tasks[0].ports[1])
broker=$(curl -s ${hubip}:${hubport}/v1/connection | jq .address[0] | tr -d '"')
satip=$(curl -s http://m1:8080/v2/apps/sattasks/${satname}/apps/sat | jq '.app.tasks[0].ipAddresses[0].ipAddress' | tr -d '"')
satport=$(curl -s http://m1:8080/v2/apps/sattasks/${satname}/apps/sat | jq '.app.tasks[0].ports[0]')
elasticsearch=$(curl -s ${satip}:${satport}/v1/tasks | jq .[0].http_address | tr -d '"')
echo "broker=$broker"
echo "elsticsearch=${elasticsearch}"

cat > getcounts.sh << EOL
#!/bin/bash

kafkats=\$(date +%s)
kafkacnt=\$(~/kafka_2.11-0.10.0.1/bin/kafka-run-class.sh kafka.tools.GetOffsetShell --broker-list ${broker} --topic ${topic} --time -1 | cut -d ':' -f 3)

elasticts=\$(date +%s)
elasticcnt=\$(curl -s http://${elasticsearch}/${datasetname}/${datasetname}/_count | jq .count)

echo \${1},\${kafkats},\${kafkacnt},\${elasticts},\${elasticcnt}
EOL

echo "If successful; then run 'bash getcounts.sh'"
echo "You can optionally add a quoted message to prepend to the results (e.g. \"Start Count\""
