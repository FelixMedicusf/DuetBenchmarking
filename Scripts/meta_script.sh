#!bin/bash

cassandra_vm=$1
ycsb_vm=$2
cassandra_script=$3
ycsb_script=$4


# Setup of cloud infrastructure
if (( $cassandra_vm==1 )); then  
cd ~/Projekt/Terraform/Cassandra_Docker_GCE && terraform init ; terraform apply -auto-approve
fi


if (( $ycsb_vm==1 )); then  
cd ~/Projekt/Terraform/YCSB_GCE && terraform init ; terraform apply -auto-approve
fi

# Provisioning of VMs and benchmark execution 

if (( $cassandra_script==1 && $ycsb_script==1 )); then
cd ~/Projekt/Scripts && sh combined_script.sh
fi

if (( $cassandra_script==1 &&$ycsb_script==0 )); then
cd ~/Projekt/Scripts && sh docker_script_new.sh
fi

if (( $ycsb_script==1 && $cassandra_script==0 )); then
cd ~/Projekt/Scripts && sh ycsb_script.sh
fi




