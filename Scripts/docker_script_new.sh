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
currentInstanceName="${instanceGroupName}-$i"
nodeExternalIp="$(gcloud compute instances describe $currentInstanceName --zone='europe-west1-b' --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
nodeInternalIp="$(gcloud compute instances describe $currentInstanceName --zone='europe-west1-b' --format='get(networkInterfaces[0].networkIP)')"
echo $currentInstanceName


# Copy data.cql from host to home directory of VM 
gcloud compute scp --zone europe-west1-b ~/Documents/data.cql $currentInstanceName:~
# Create Docker Network a and b 
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-a'
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-b'

# starting the first node requires a different cassandra configuration
if [[ $i -eq 1 ]];then

seedIp=$nodeInternalIp
seedIpExternal=$nodeExternalIp
firstInstanceName=$currentInstanceName

# Cassandra uses port 7000 for Internode Communication, 7001 for TLS internode communication, 9160 as Thrift Client API, and 9042 as CQL native transport port 
# -p 8080:80 --> Map TCP port 80 in the container to port 8080 on the Docker host. -p <host_port>:<container_port>

command1 = "cd ~ && printf \"cluster_name: 'Cassandra Cluster A' \nstorage_port: 7005\" > ~/cassandraA.yaml"
command2 = "cd ~ && printf \"cluster_name: 'Cassandra Cluster B' \nstorage_port: 7010\" > ~/cassandraB.yaml"
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $command1  
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $command2

# TO DO: Replace -e variables with config file + STORAGE_PORT
cmd="sudo docker run --name cassandra-container-${i}a -d --rm\
                        --hostname cassandra-container-${i}a\
                        --network cassandra-network-a\
                        -v /docker/cassandra/container-${i}a:/var/lib/cassandra\
                        -p 9042:9042\
                        -p 7005:7005\
                        cassandra:latest\
                        -Dcassandra.config=~/cassandraA.yaml"

# Start first Container
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $cmd
echo "Started first Container (${i}a) on port 7005 and 9042 in ${currentInstanceName}"


# TO DO: Replace -e variables with config file + STORAGE_PORT
cmd="sudo docker run --name cassandra-container-${i}b -d --rm\
                        --hostname cassandra-container-${i}b\
                        --network cassandra-network-b\
                        -v /docker/cassandra/container-${i}b:/var/lib/cassandra\
                        -p 9043:9042\
                        -p 7010:7010\
                        cassandra:3.11\
                        -Dcassandra.config=~/cassandraB.yaml"

# Start second Container
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $cmd
echo "Started second Container (${i}b) on port 7010 and 9043 in ${currentInstanceName}"

fi

# for subsequent nodes we need to provide a cassandra seed (IP address of first deployed node) for nodes to join the cluster
if [[ $i -ne 1 ]]; then 

# TO DO: Write script to add the seed_ip addresses to the cassandra1.yaml and cassandra2.yaml and then pass as argument to docker run (mount in container)

if [[$i -eq 2]];then
command1 = "cd ~ && printf \"\nseed_provider:\n  - class_name: org.apache.cassandra.locator.SimpleSeedProvider\n    - seeds: $seedIp\" >> ~/cassandraA.yaml"
command2 = "cd ~ && printf \"\nseed_provider:\n  - class_name: org.apache.cassandra.locator.SimpleSeedProvider\n    - seeds: $seedIp\" >> ~/cassandraB.yaml"
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $command1
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $command2

fi

# Start first Container
# TO DO: Replace -e variables with config file + STORAGE_PORT
cmd="sudo docker run --name cassandra-container-${i}a -d --rm\
                        --hostname cassandra-container-${i}a\
                        --network cassandra-network-a\
                        -v /docker/cassandra/container-${i}a:/var/lib/cassandra\
                        -p 9042:9042\
                        -p 7005:7005\
                        cassandra:latest\
                        -Dcassandra.config=~/cassandraA.yaml"

gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $cmd
echo "Started first Container (${i}a) on port 7005 and 9042 in ${currentInstanceName}"

# TO DO: Replace -e variables with config file + STORAGE_PORT
cmd="sudo docker run --name cassandra-container-${i}b -d --rm\
                        --hostname cassandra-container-${i}b\
                        --network cassandra-network-b\
                        -v /docker/cassandra/container-${i}b:/var/lib/cassandra\
                        -p 9043:9042\
                        -p 7010:7010\
                        cassandra:3.11\
                        -Dcassandra.config=~/cassandraB.yaml"
# Start second Container
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- $cmd
echo "Started second Container (${i}b) on port 7010 and 9043 in ${currentInstanceName}"

fi

echo "Waiting for Container ${i}a and ${i}b to run to load data"
sleep 35 

# Multiple attempts to load data (keyspace and table) into cassandra-container-${i}a
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-a --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-${i}a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh"
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-a --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-${i}a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh"


# Multiple attempts to load data (keyspace and table) into cassandra-container-${i}b
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-b --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-${i}b -e CQLSH_PORT=9042 -e CQLVERSION=3.4.4 nuvo/docker-cqlsh"
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-b --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-${i}b -e CQLSH_PORT=9042 -e CQLVERSION=3.4.4 nuvo/docker-cqlsh"

# Show active Docker Container
printf "Active Docker Container in VM: $currentInstanceName\n"
gcloud compute ssh $currentInstanceName --zone europe-west1-b -- 'sudo docker ps'

done

printf "Nodes of Cluster A:\n"
gcloud compute ssh $firstInstanceName --zone europe-west1-b -- 'sudo docker exec -it cassandra-container-1a nodetool status'

printf "Nodes of Cluster B:\n"
gcloud compute ssh $firstInstanceName --zone europe-west1-b -- 'sudo docker exec -it cassandra-container-1b nodetool status'

sudo rm  ~/cassandraA.yaml
sudo rm  ~/cassandraB.yaml

# Erkennt Cassandra den jeweligen Port über den Namen des Cluster auf der seed_ip? oder immer 7000?