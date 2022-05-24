#!/bin/bash
read  -p "Enter Instance Name in which you want to deploy Cassandra Docker Container: " instanceName
instanceName=${name:-cassandra-instance-1}

# Create Docker Network a and b 
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-a'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-b'

# Copy data.cql from host to home directory of VM 
gcloud compute scp --zone europe-west1-b ~/Documents/data.cql $instanceName:~


# Cassandra uses port 7000 for Internode Communication, 7001 for TLS internode communication, 9160 as Thrift Client API, and 9042 as CQL native transport port 
# -p 8080:80 --> Map TCP port 80 in the container to port 8080 on the Docker host. -p <host_port>:<container_port>

# Start first Container
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker run -d --rm --name cassandra-container-1a --hostname cassandra-container-1a --network cassandra-network-a -v /docker/cassandra/container-1a:/var/lib/cassandra -p 9042:9042 -p 7000:7000 cassandra:latest'

echo "Started second Container (1a) on port 7000 and 9042"
echo "Waiting for Container 1a to run to load data"
sleep 20 

# Load CQL file into cassandra-container-1a
# CAUTION: CQLVERSION must fit to respective Cassandra Version
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker run --rm --network cassandra-network-a -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh'


# Start second Container
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker run -d --rm --name cassandra-container-1b --hostname cassandra-container-1b --network cassandra-network-b -v /docker/cassandra/container-1b:/var/lib/cassandra -p 9043:9042 -p 7001:7000 cassandra:3.11'

echo "Started second Container (1b) on port 7001 and 9043"
echo "Waiting for Container 1b to run to load data"
sleep 20 

# Load CQL file into cassandra-container-1b
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker run --rm --network cassandra-network-b -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1b -e CQLSH_PORT=9042 -e CQLVERSION=3.4.4 nuvo/docker-cqlsh'

gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker run --rm --network cassandra-network-a -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh'

# Show active Docker Container
echo "Active Docker Container in VM: $instanceName \n"
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo docker ps'