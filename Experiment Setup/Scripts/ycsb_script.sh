#!/bin/bash
read  -p "Enter Instance Name in which you want to deploy the YCSB Benchmarking Client: " instanceName
instanceName=${name:-ycsb-instance-1}

read -p "SUT already deployed (y/n)?: " CONT

if [ "$CONT" = "y" ];then 
    read  -p "Enter Instance Name of SUT: " SUTinstanceName
    # Specify default value for the SUTinstanceName (cassandra-instance-1)
    SUTinstanceName=${name:-cassandra-instance-1}

    # Get IP address of SUT 
    SUT_IP="$(gcloud compute instances describe $SUTinstanceName --zone='europe-west1-b' --format='get(networkInterfaces[0].accessConfigs[0].natIP)')"
    echo "SUT IP is " $SUT_IP
fi


# Install Python 2.7 and make it default for executing pyhton scripts
gcloud compute ssh $instanceName --zone europe-west1-b -- 'yes | sudo apt install python2.7'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo update-alternatives --install /usr/bin/python python /usr/bin/python2.7 1'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo update-alternatives --install /usr/bin/python python /usr/bin/python3 2'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'echo "/usr/bin/python2.7" | sudo update-alternatives --config python'


# Get and unpack YCSB
gcloud compute ssh $instanceName --zone europe-west1-b -- 'cd ~ && sudo curl -O --location https://github.com/brianfrankcooper/YCSB/releases/download/0.17.0/ycsb-0.17.0.tar.gz'
gcloud compute ssh $instanceName --zone europe-west1-b -- 'sudo tar xfvz ycsb-0.17.0.tar.gz'


if [ "$CONT" = "y" ];then
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
fi
# The standard workload parameter files create very small databases. For example, workloada only creates 1000 records. Useful for debugging ....
# To increase just set the recordcount parameter in load and run phase. 

# -threads and -target parameter to control the amount of offered load.
# When not using command line arguments the large.dat file can be used as -P large.dat

# Specify measurementtype=timeseries in run command to make the Client report average latency for each interval of 1000 miliseconds. 