#!/bin/bash
echo "##########################################################################################################################"
echo "Cassandra-Setup"
read  -p "Enter Instance Name in which you want to deploy Cassandra Docker Container: " cassandraInstanceName
cassandraInstanceName=${name:-cassandra-instance-1}

# Create Docker Network a and b 
gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-a'
gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker network create cassandra-network-b'

# Copy data.cql from host to home directory of VM 
gcloud compute scp --zone europe-west1-b ~/Documents/data.cql $cassandraInstanceName:~


# Cassandra uses port 7000 for Internode Communication, 7001 for TLS internode communication, 9160 as Thrift Client API, and 9042 as CQL native transport port 
# -p 8080:80 --> Map TCP port 80 in the container to port 8080 on the Docker host. -p <host_port>:<container_port>

# Start first Container
gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker run -d --rm --name cassandra-container-1a --hostname cassandra-container-1a --network cassandra-network-a -v /docker/cassandra/container-1a:/var/lib/cassandra -p 9042:9042 -p 7000:7000 cassandra:latest'

echo "Started second Container (1a) on port 7005 and 9042"
echo "Waiting for Container 1a to run to load data"
sleep 20 

# Load CQL file into cassandra-container-1a
# CAUTION: CQLVERSION must fit to respective Cassandra Version
gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker run --rm --network cassandra-network-a -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh'


# Start second Container
gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker run -d --rm --name cassandra-container-1b --hostname cassandra-container-1b --network cassandra-network-b -v /docker/cassandra/container-1b:/var/lib/cassandra -p 9043:9042 -p 7001:7000 cassandra:3.11'

echo "Started second Container (1b) on port 7010 and 9043"
echo "Waiting for Container 1b to run to load data"
sleep 20 

# Load CQL file into cassandra-container-1b
gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker run --rm --network cassandra-network-b -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1b -e CQLSH_PORT=9042 -e CQLVERSION=3.4.4 nuvo/docker-cqlsh'

gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker run --rm --network cassandra-network-a -v ~/data.cql:/scripts/data.cql -e CQLSH_HOST=cassandra-container-1a -e CQLSH_PORT=9042 -e CQLVERSION=3.4.5 nuvo/docker-cqlsh'

# Show active Docker Container
echo "Active Docker Container in VM: $cassandraInstanceName \n"
gcloud compute ssh $cassandraInstanceName --zone europe-west1-b -- 'sudo docker ps'

echo "Finished Cassandra Setup"
echo "##########################################################################################################################"
echo "Starting YCSB Client"

read  -p "Enter Instance Name in which you want to deploy the YCSB Benchmarking Client: " instanceName
instanceName=${name:-ycsb-instance-1}

# Get IP address of SUT 
SUT_IP="$(gcloud compute instances describe $cassandraInstanceName --zone='europe-west1-b' --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
echo "SUT IP is " $SUT_IP


# Install Python 2.7 and make it default for executing pyhton scripts
gcloud compute ssh $instanceName --zone europe-west1-b -- 'yes | sudo apt install python2.7'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo update-alternatives --install /usr/bin/python python /usr/bin/python2.7 1'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo update-alternatives --install /usr/bin/python python /usr/bin/python3 2'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'echo "/usr/bin/python2.7" | sudo update-alternatives --config python'


# Get and unpack YCSB
gcloud compute ssh $instanceName --zone europe-west1-b -- 'cd ~ && sudo curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo tar xfvz ycsb-0.17.0.tar.gz'



echo "Loading phase cassandra-container-1a"
gcloud compute ssh $instanceName --zone europe-west1-b -- "cd ~/ycsb-0.17.0 && ./bin/ycsb load cassandra-cql -p hosts=$SUT_IP -p recordcount=50000 -s -P workloads/workloada"

# Transaction Phase for cassandra-container-1a
# Results are being written into ~/results_new.dat and being copied to host machine
echo "Transaction phase cassandra-container-1a"
gcloud compute ssh $instanceName --zone europe-west1-b -- "cd ~/ycsb-0.17.0 && ./bin/ycsb run cassandra-cql -p hosts=$SUT_IP -p recordcount=50000 -s -P workloads/workloada > ~/results_new.dat"
gcloud compute scp --zone europe-west1-b --recurse ycsb-instance-1:~/results_new.dat ~/Documents/results_new.dat



echo "Loading phase cassandra-container-1b"
gcloud compute ssh $instanceName --zone europe-west1-b -- "cd ~/ycsb-0.17.0 && ./bin/ycsb load cassandra-cql -p hosts=$SUT_IP -p ports=9043 -p recordcount=50000 -s -P workloads/workloada"

# Transaction Phase for cassandra-container-1b
# Results are being written into ~/results_old.dat and being copied to host machine
echo "Transaction phase cassandra-container-1b"
gcloud compute ssh $instanceName --zone europe-west1-b -- "cd ~/ycsb-0.17.0 && ./bin/ycsb run cassandra-cql -p hosts=$SUT_IP -p ports=9043 -p recordcount=50000 -s -P workloads/workloada > ~/results_old.dat"
gcloud compute scp --zone europe-west1-b --recurse ycsb-instance-1:~/results_old.dat ~/Documents/results_old.dat


echo "YCSB Client stopped executing workload.. Results written to ~/Documents/results_new.dat and ~/Documents/results_old.dat on host machine"
echo "##########################################################################################################################"