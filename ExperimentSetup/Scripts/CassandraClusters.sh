#!/bin/bash

# Get number of cassandra nodes
instances="$(gcloud compute instances list)"
value="cassandra"
nodeNumber="$(echo -n $instances | grep -Fo $value | wc -l)"

instanceGroupName=${name:-cassandra-node}
read  -p "Enter Instance group name in which you want to deploy Cassandra Docker Container Network: " instanceGroupName

# Loop through all deployed nodes to provision them with Cassandra Container
for (( i=1; i <= $nodeNumber; ++i ))
do 
firstInstanceName="${instanceGroupName}-1"
currentInstanceName="${instanceGroupName}-$i"
zone="$(gcloud compute instances list --filter="name=$currentInstanceName" --format "get(zone)" | awk -F/ '{print $NF}')"
nodeExternalIp="$(gcloud compute instances describe $currentInstanceName --zone=$zone --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
nodeInternalIp="$(gcloud compute instances describe $currentInstanceName --zone=$zone --format='get(networkInterfaces[0].networkIP)')"

echo "Provisioning $currentInstanceName"


# Create Docker Network a and b 
gcloud compute ssh $currentInstanceName --zone $zone -- 'sudo docker network create cassandra-network-a'
gcloud compute ssh $currentInstanceName --zone $zone -- 'sudo docker network create cassandra-network-b'

# starting the first node requires a different cassandra configuration
if [[ $i -eq 1 ]];then

# Copy data.cql from host to home directory of VM 
gcloud compute scp --zone $zone ~/Documents/DuetBenchmarking/ExperimentSetup/Scripts/data.cql $currentInstanceName:~

firstZone=$zone
seedIp=$nodeInternalIp
seedIpExternal=$nodeExternalIp
firstInstanceName=$currentInstanceName

# Cassandra uses port 7000 for Internode Communication, 7001 for TLS internode communication, 9160 as Thrift Client API, and 9042 as CQL native transport port 
# -p 8080:80 --> Map TCP port 80 in the container to port 8080 on the Docker host. -p <host_port>:<container_port>

# command1="cd ~ && printf \"cluster_name: 'Cassandra Cluster A' \nstorage_port: 7005\" > /home/felixmedicus/cassandraA.yaml"
# command2="cd ~ && printf \"cluster_name: 'Cassandra Cluster B' \nstorage_port: 7010\" > /home/felixmedicus/cassandraB.yaml"
# gcloud compute ssh $currentInstanceName --zone $zone-b -- $command1  
# gcloud compute ssh $currentInstanceName --zone $zone-b -- $command2

cmd="sudo docker run --name cassandra-container-${i}a -d --rm\
                        --hostname cassandra-container-${i}a\
                        --network cassandra-network-a\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeInternalIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster A'\
                        -e CASSANDRA_STORAGE_PORT=7005\
                        -e CASSANDRA_NATIVE_TRANSPORT_PORT=9045\
                        -e CASSANDRA_BROADCAST_RPC_ADDRESS=$nodeExternalIp\
                        -e CASSANDRA_ENDPOINT_SNITCH='GoogleCloudSnitch'\
                        -e CASSANDRA_NUM_TOKENS=16\
                        -v /docker/cassandra/container-${i}a:/var/lib/cassandra\
                        -p 9045:9045\
                        -p 7005:7005\
                        felixmedicus/cassandra_edited:4.0.4"

# Start first Container
gcloud compute ssh $currentInstanceName --zone $zone -- $cmd
echo "Started first Container (${i}a) on port 7005 and 9045 in ${currentInstanceName}"

cmd="sudo docker run --name cassandra-container-${i}b -d --rm\
                        --hostname cassandra-container-${i}b\
                        --network cassandra-network-b\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeInternalIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster B'\
                        -e CASSANDRA_STORAGE_PORT=7010\
                        -e CASSANDRA_NATIVE_TRANSPORT_PORT=9050\
                        -e CASSANDRA_BROADCAST_RPC_ADDRESS=$nodeExternalIp\
                        -e CASSANDRA_ENDPOINT_SNITCH='GoogleCloudSnitch'\
                        -e CASSANDRA_NUM_TOKENS=16\
                        -v /docker/cassandra/container-${i}b:/var/lib/cassandra\
                        -p 9050:9050\
                        -p 7010:7010\
                        felixmedicus/cassandra_edited:4.0.4"

# Start second Container
gcloud compute ssh $currentInstanceName --zone $zone -- $cmd
echo "Started second Container (${i}b) on port 7010 and 9050 in ${currentInstanceName}"

fi

# for subsequent nodes we need to provide a cassandra seed (IP address of first deployed node) for nodes to join the cluster
if [[ $i -ne 1 ]]; then 

sleep 10 

# if [[ $i -eq 2 ]];then
# command1="cd ~ && printf \"\nseed_provider:\n  - class_name: org.apache.cassandra.locator.SimpleSeedProvider\n    - seeds: $seedIp\" >> /home/felixmedicus/cassandraA.yaml"
# command2="cd ~ && printf \"\nseed_provider:\n  - class_name: org.apache.cassandra.locator.SimpleSeedProvider\n    - seeds: $seedIp\" >> /home/felixmedicus/cassandraB.yaml"
# gcloud compute ssh $currentInstanceName --zone $zone-b -- $command1
# gcloud compute ssh $currentInstanceName --zone $zone-b -- $command2

# fi

# Start first Container
cmd="sudo docker run --name cassandra-container-${i}a -d --rm\
                        --hostname cassandra-container-${i}a\
                        --network cassandra-network-a\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeInternalIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster A'\
                        -e CASSANDRA_SEEDS=$seedIp\
                        -e CASSANDRA_STORAGE_PORT=7005\
                        -e CASSANDRA_NATIVE_TRANSPORT_PORT=9045\
                        -e CASSANDRA_BROADCAST_RPC_ADDRESS=$nodeExternalIp\
                        -e CASSANDRA_ENDPOINT_SNITCH='GoogleCloudSnitch'\
                        -e CASSANDRA_NUM_TOKENS=16\
                        -v /docker/cassandra/container-${i}a:/var/lib/cassandra\
                        -p 9045:9045\
                        -p 7005:7005\
                        felixmedicus/cassandra_edited:4.0.4"

gcloud compute ssh $currentInstanceName --zone $zone -- $cmd
echo "Started first Container (${i}a) on port 7005 and 9045 in ${currentInstanceName}"

cmd="sudo docker run --name cassandra-container-${i}b -d --rm\
                        --hostname cassandra-container-${i}b\
                        --network cassandra-network-b\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeInternalIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster B'\
                        -e CASSANDRA_SEEDS=$seedIp\
                        -e CASSANDRA_STORAGE_PORT=7010\
                        -e CASSANDRA_NATIVE_TRANSPORT_PORT=9050\
                        -e CASSANDRA_BROADCAST_RPC_ADDRESS=$nodeExternalIp\
                        -e CASSANDRA_ENDPOINT_SNITCH='GoogleCloudSnitch'\
                        -e CASSANDRA_NUM_TOKENS=16\
                        -v /docker/cassandra/container-${i}b:/var/lib/cassandra\
                        -p 9050:9050\
                        -p 7010:7010\
                        felixmedicus/cassandra_edited:4.0.4"
# Start second Container
gcloud compute ssh $currentInstanceName --zone $zone -- $cmd
echo "Started second Container (${i}b) on port 7010 and 9050 in ${currentInstanceName}"

fi

# Show active Docker Container
printf "Active Docker Container in VM: $currentInstanceName\n"
gcloud compute ssh $currentInstanceName --zone $zone -- 'sudo docker ps'

done


sleep 15

# Load data (keyspace and table) into Cluster A (cassandra-container-1a) 
gcloud compute ssh $firstInstanceName --zone $firstZone -- "sudo docker run --network cassandra-network-a --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9045 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh"

# Load data (keyspace and table) into Cluster B (cassandra-container-1b)
gcloud compute ssh $firstInstanceName --zone $firstZone -- "sudo docker run --network cassandra-network-b --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1b -e CQLSH_PORT=9050 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh"

printf "Nodes of Cluster A:\n"
gcloud compute ssh $firstInstanceName --zone $firstZone -- 'sudo docker exec -it cassandra-container-1a nodetool status'

printf "Nodes of Cluster B:\n"
gcloud compute ssh $firstInstanceName --zone $firstZone -- 'sudo docker exec -it cassandra-container-1b nodetool status'
