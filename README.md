# DuetBenchmarking


## Terraform 
- First you need to setup the cloud infrastructure for the SUT. To do so go into the ``DuetBenchmarking/Terraform/Cassandra_Docker_GCE`` directory and run ``terraform init``. After run ``terraform apply`` to setup the first part of the cloud setup. 
- After that you need to set up the Virtual Machine which will act as the Benchmarking Client. Just change into the ``DuetBenchmarking/Terraform/YCSB_GCE`` directory and run ``terraform init`` followed by ``terraform apply``. 
- After the benchmark was conducted, you can tear the infrastructure down by applying ``terraform destroy`` in the respective directory.



## Scripts
- To provision both Virtual Machines which were set up in the first step, you must run the ``/Scripts/combined_script.sh``, or first run the ``Scripts/docker_script.sh`` followed by the ``Scripts/docker_script.sh``.
- After the Benchmark is performed, do not forget to tear down the Cloud Infrastructure by running ``terraform destroy`` in the respective directories. 