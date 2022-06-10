#!/bin/bash
instanceName=${name:-ycsb-instance-1}
read  -p "Enter Instance Name in which you want to deploy the YCSB Benchmarking Client: " instanceName

SUTInstanceGroupName=${name:-cassandra-node}
read  -p "Enter Instance group name on which you want to perform the Duet Benchmark: " SUTInstanceGroupName

# Get number of cassandra nodes
instances="$(gcloud compute instances list)"
value="cassandra"
nodeNumber="$(echo -n $instances | grep -Fo $value | wc -l)"

externalIpArray=()

for (( i=1; i <= $nodeNumber; ++i ))
do 
currentInstanceName="${SUTInstanceGroupName}-$i"
nodeExternalIp="$(gcloud compute instances describe $currentInstanceName --zone='europe-west1-b' --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
nodeInternalIp="$(gcloud compute instances describe $currentInstanceName --zone='europe-west1-b' --format='get(networkInterfaces[0].networkIP)')"
externalIpArray+=($nodeExternalIp)
done

echo "${externalIpArray[@]}"

# Install Python 2.7 and make it default for executing python scripts
gcloud compute ssh $instanceName --zone europe-west1-b -- 'yes | sudo apt install python2.7'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo update-alternatives --install /usr/bin/python python /usr/bin/python2.7 1'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo update-alternatives --install /usr/bin/python python /usr/bin/python3 2'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'echo "/usr/bin/python2.7" | sudo update-alternatives --config python'

# Get and unpack YCSB
gcloud compute ssh $instanceName --zone europe-west1-b -- 'cd ~ && sudo curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo tar xfvz ycsb-0.17.0.tar.gz'

# Generate Operations for later injection into database 
gcloud compute ssh $instanceName --zone europe-west1-b -- 'cd ~/ycsb-0.17.0 && ./bin/ycsb load basic -P workloads/workloada > ~/load_operations.dat'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'cd ~/ycsb-0.17.0 && ./bin/ycsb load basic -P workloads/workloada > ~/run_operations.dat'



# Copy Kotlin code to machine 
# run kotlin code with ip address array, and put load and run operations in folder or also give as parameter 