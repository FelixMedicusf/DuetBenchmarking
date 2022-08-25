#!/bin/bash
# Set the totalNumberOfOperations as Script Input Parameter (default value=10000)
totalNumberOfOperations="${1:-1000000}"

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
cd ~/ycsb-0.17.0 && ./bin/ycsb load basic -P workloads/workloadc -p operationcount=$totalNumberOfOperations -p recordcount=$totalNumberOfOperations > ~/Documents/DuetBenchmarking/load_operations.dat
cd ~/ycsb-0.17.0 && ./bin/ycsb run basic -P workloads/workloadc -p operationcount=$totalNumberOfOperations -p recordcount=$totalNumberOfOperations > ~/Documents/DuetBenchmarking/run_operations.dat

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


# Get BenchmarkingManager jar and execute load and then run phase
git clone https://github.com/FelixMedicusf/DuetBenchmarking
cd ~/DuetBenchmarking/BenchmarkingClient/BenchmarkingManager/jar && java -jar BenchmarkingWorker-final-worker.jar -n $cassandraExternalIps -w $workerExternalIps -r $cassandraRegions -wl ~/Documents/DuetBenchmarking/load_operations.dat
cd ~/DuetBenchmarking/BenchmarkingClient/BenchmarkingManager/jar && java -jar BenchmarkingWorker-final-worker.jar -n $cassandraExternalIps -w $workerExternalIps -r $cassandraRegions -wl ~/Documents/DuetBenchmarking/run_operations.dat -run