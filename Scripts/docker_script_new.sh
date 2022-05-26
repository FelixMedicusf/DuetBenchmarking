#!/bin/bash

# Get number of cassandra nodes
instances="$(gcloud compute instances list)"
value="cassandra"
nodeNumber="$(echo -n $instances | grep -Fo $value | wc -l)"

instanceGroupName=${name:-cassandra-node}
read  -p "Enter Instance group name in which you want to deploy Cassandra Docker Container Network: " instanceGroupName

for (( i=1; i <= $nodeNumber; ++i ))
do 
instanceName="${instanceGroupName}-$i"
nodeIp="$(gcloud compute instances describe $instanceName --zone='europe-west1-b' --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
echo $instanceName


# Copy data.cql from host to home directory of VM 
gcloud compute scp --zone europe-west1-b ~/Documents/data.cql $instanceName:~
# Create Docker Network a and b 
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-a'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-b'

# starting the first node requires a different cassandra configuration
if [[ $i -eq 1 ]];then

seedIp=$nodeIp


 
# Cassandra uses port 7000 for Internode Communication, 7001 for TLS internode communication, 9160 as Thrift Client API, and 9042 as CQL native transport port 
# -p 8080:80 --> Map TCP port 80 in the container to port 8080 on the Docker host. -p <host_port>:<container_port>


cmd="sudo docker run --name cassandra-container-1a -d --rm\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster Version 1'\
                        --hostname cassandra-container-1a\
                        --network cassandra-network-a\
                        -v /docker/cassandra/container-1a:/var/lib/cassandra\
                        -p 9042:9042\
                        -p 7000:7000\
                        cassandra:latest"

# Start first Container
gcloud compute ssh $instanceName --zone europe-west1-b -- $cmd


echo "Started second Container (1a) on port 7000 and 9042"

cmd="sudo docker run --name cassandra-container-1b -d --rm\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster Version 2'\
                        --hostname cassandra-container-1b\
                        --network cassandra-network-b\
                        -v /docker/cassandra/container-1b:/var/lib/cassandra\
                        -p 9043:9042\
                        -p 7001:7000\
                        cassandra:3.11"

# Start second Container
gcloud compute ssh $instanceName --zone europe-west1-b -- $cmd

fi

# for subsequent nodes we need to provide a cassandra seed for nodes to join the cluster
if [[ $i -ne 1 ]]; then 
# Start first Container
cmd="sudo docker run --name cassandra-container-1a -d --rm\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeIp\
                        -e CASSANDRA_SEEDS=$seedIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster Version 1'\
                        --hostname cassandra-container-1a\
                        --network cassandra-network-a\
                        -v /docker/cassandra/container-1a:/var/lib/cassandra\
                        -p 9042:9042\
                        -p 7000:7000\
                        cassandra:latest"

gcloud compute ssh $instanceName --zone europe-west1-b -- $cmd
echo "Started first Container (1a) on port 7000 and 9042"

cmd="sudo docker run --name cassandra-container-1b -d --rm\
                        -e CASSANDRA_BROADCAST_ADDRESS=$nodeIp\
                        -e CASSANDRA_SEEDS=$seedIp\
                        -e CASSANDRA_CLUSTER_NAME='Cassandra Cluster Version 2'\
                        --hostname cassandra-container-1b\
                        --network cassandra-network-b\
                        -v /docker/cassandra/container-1b:/var/lib/cassandra\
                        -p 9043:9042\
                        -p 7001:7000\
                        cassandra:3.11"
# Start second Container
gcloud compute ssh $instanceName --zone europe-west1-b -- $cmd
echo "Started second Container (1b) on port 7001 and 9043"

fi


echo "Waiting for Container 1a and 1b to run to load data"
sleep 35 

# Multiple attempts to load data (keyspace and table) into cassandra-container-1a
gcloud compute ssh $instanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-a --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh"
gcloud compute ssh $instanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-a --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh"
gcloud compute ssh $instanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-a --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh"

# Multiple attempts to load data (keyspace and table) into cassandra-container-1a
gcloud compute ssh $instanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-b --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1b -e CQLSH_PORT=9042 -e CQLVERSION=3.4.4 nuvo/docker-cqlsh"
gcloud compute ssh $instanceName --zone europe-west1-b -- "sudo docker run --network cassandra-network-b --rm -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1b -e CQLSH_PORT=9042 -e CQLVERSION=3.4.4 nuvo/docker-cqlsh"

# Show active Docker Container
printf "Active Docker Container in VM: $instanceName\n"
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker ps'
done

printf "Nodes of Cluster A:\n"
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker exec -it cassandra-container-1a nodetool status'

printf "Nodes of Cluster B:\n"
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker exec -it cassandra-container-1b nodetool status'