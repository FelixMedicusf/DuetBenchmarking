#!/bin/bash
# Set the totalNumberOfOperations as Script Input Parameter (default value=10000)
totalNumberOfOperations="${1:-10000}"


echo "Specified number of operations: $totalNumberOfOperations"
# Install Python 2.7 and make it default for executing pyhton scripts
yes | sudo apt install python2.7
sudo update-alternatives --install /usr/bin/python python /usr/bin/python2.7 1
sudo update-alternatives --install /usr/bin/python python /usr/bin/python3 2
echo "/usr/bin/python2.7" | sudo update-alternatives --config python


# Get and unpack YCSB
cd ~ && sudo curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz
sudo tar xfvz ycsb-0.17.0.tar.gz


# Generate Operations for later injection into database 
cd ~/ycsb-0.17.0 && ./bin/ycsb load basic -P workloads/workloada -p recordcount=$totalNumberOfOperations > ~/Documents/DuetBenchmarking/load_operations.dat
cd ~/ycsb-0.17.0 && ./bin/ycsb run basic -P workloads/workloada -p recordcount=$totalNumberOfOperations > ~/Documents/DuetBenchmarking/run_operations.dat

# Fetches the Ip-addresses and regions of the VMs on which cassandra is deployed
instances="$(gcloud compute instances list)"
value="cassandra"
numberOfCassandraNodes="$(echo -n $instances | grep -Fo $value | wc -l)"

SUTInstanceGroupName="cassandra-node"

cassandraExternalIps=()
cassandraRegions=()

for (( i=1; i <= $numberOfCassandraNodes; ++i ))
do 
currentInstanceName="${SUTInstanceGroupName}-$i"
zone="$(gcloud compute instances list --filter="name=$currentInstanceName" --format "get(zone)" | awk -F/ '{print $NF}')"
region=${zone%-*}
nodeExternalIp="$(gcloud compute instances describe $currentInstanceName --zone=$zone --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
cassandraExternalIps+=($nodeExternalIp)
cassandraRegions+=($region)
done

echo "${cassandraExternalIps[@]}"
echo "${cassandraRegions[@]}"

# Fetches the Ip-addresses of the worker VMs
instances="$(gcloud compute instances list)"
value="worker"
numberOfWorkerNodes="$(echo -n $instances | grep -Fo $value | wc -l)"

workerInstanceGroupName="worker"

workerExternalIps=()

for (( i=1; i <= $numberOfWorkerNodes; ++i ))
do 
currentInstanceName="${workerInstanceGroupName}-$i"
zone="$(gcloud compute instances list --filter="name=$currentInstanceName" --format "get(zone)" | awk -F/ '{print $NF}')"
nodeExternalIp="$(gcloud compute instances describe $currentInstanceName --zone=$zone --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
workerExternalIps+=($nodeExternalIp)
done

echo "${workerExternalIps[@]}"



# Get git repo and start Benchmark Manager with parameters
# TODO: replace name of jar file and parameters
# Parameters: cassandra_node_ips, cassandra-node-frequencies, cassandra-node-regions, worker-ips, total_number_of_operations (optional), Path to workload (5 bzw. 6)
# cassandraNodeIps = [] (cassandraExternalIps) (array)
# cassandraNodeFrequencies = [] 
# cassandraNodeRegions = [] (cassandraRegions) (array)
# workerIps = [] (workerExternalIps) (array)
# totalNumberOfOperations = [] (totalNumberOfOperations) (number? int?)
# pathToWorkload = [] ("~/load_operations.dat" oder "~/run_operations.dat")

# Format java -jar <jar_name> 
# -n 34.77.58.26,34.142.60.76,34.142.60.76 -w 35.205.205.137,35.189.111.242,34.159.113.65 -l 10000 
# -f 0.4,0.5,0.6 -r europe-west1-b,europe-west2-b,europe-west3-b -wl Documents/DuetBenchmarking/load_operations.dat


# git clone https://github.com/FelixMedicusf/DuetBenchmarking
# cd ~/DuetBenchmarking/BenchmarkingClient/BenchmarkingManager/jar && java -jar BenchmarkingWorker-final-worker.jar -n 34.77.58.26,34.142.60.76,34.142.60.76 -w 35.205.205.137,35.189.111.242,34.159.113.65 -l 10000 -f 0.4,0.5,0.6 -r europe-west1-b,europe-west2-b,europe-west3-b -wl Documents/DuetBenchmarking/load_operations.dat
