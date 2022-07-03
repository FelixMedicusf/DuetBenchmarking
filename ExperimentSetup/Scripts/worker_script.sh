#!/bin/bash

# Get number of cassandra nodes
instances="$(gcloud compute instances list)"
value="worker"
nodeNumber="$(echo -n $instances | grep -Fo $value | wc -l)"

instanceGroupName=${name:-worker}
read  -p "Enter Instance group name in which you want to deploy Workers: " instanceGroupName

# Loop through all deployed nodes to provision them with Cassandra Container
for (( i=1; i <= $nodeNumber; ++i ))
do 
firstInstanceName="${instanceGroupName}-1"
currentInstanceName="${instanceGroupName}-$i"
zone="$(gcloud compute instances list --filter="name=$currentInstanceName" --format "get(zone)" | awk -F/ '{print $NF}')"
nodeExternalIp="$(gcloud compute instances describe $currentInstanceName --zone=$zone --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
nodeInternalIp="$(gcloud compute instances describe $currentInstanceName --zone=$zone --format='get(networkInterfaces[0].networkIP)')"

echo "Starting Worker in $currentInstanceName"

gcloud compute ssh --zone $zone $currentInstanceName -- 'sudo apt update && sudo apt install git'
gcloud compute ssh --zone $zone $currentInstanceName -- 'git clone https://github.com/FelixMedicusf/DuetBenchmarking'
gcloud compute ssh --zone $zone $currentInstanceName -- 'cd ~/DuetBenchmarking/BenchmarkingClient/BenchmarkingWorker/jar && java -jar BenchmarkingWorker-final-worker.jar'





done
