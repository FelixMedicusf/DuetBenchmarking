# DuetBenchmarking


![Duet Benchmarking Architecture](https://user-images.githubusercontent.com/60180336/187493371-4957d94c-47b9-40e6-8b20-4ee5fddcf01f.jpg)


## Cloud Infrastructure 
- Terraform needs to authenticate against the Google Cloud Platform. Currently we are using a token-based authentication. 
- First you need to setup the cloud infrastructure for both clusters of the SUT. To do so, change into the ``DuetBenchmarking/ExperimentSetup/Terraform/SUT`` directory and run ``terraform init`` followed by ``terraform apply --auto-approve``. 
- After that you need to set up the VMs in which the Benchmarking Workers are deployed. To do so, change into the ``DuetBenchmarking/ExperimentSetup/Terraform/BenchmarkingWorkers`` directory and run ``terraform init`` followed by ``terraform apply --auto-approve``. 
- After the benchmark was conducted, you can tear down the cloud infrastructure by applying ``terraform destroy  --auto-approve`` in the respective directory.



## Deployment of Cassandra Clusters and Benchmarking Workers
- Prerequisite for running the the script ``CassandraClusters.sh`` is that you installed the gcloud command line tool 
- To perform an A/A-test the Docker images used in the ``CassandraClusters.sh`` file must be the same. 
- To perform A/B-tests you must change the docker image of one of the the clusters to deploy another version of Cassandra in each VM. 
- To provision both Virtual Machines which were set up in the first step, you must first run the ``Scripts/CassandraClusters`` to setup the SUT in two clusters, followed by the ``Scripts/BenchmarkingWorkers`` using ``bash``.

## YCSB 
- You need to install [YCSB](https://github.com/brianfrankcooper/YCSB), as it is used to generate database operations for the load and the run phases. 



## Conducting Experiments 
- To run the benchmarking manager you need to specify following parameters: 
![Benchmarking Manager Parameters](https://user-images.githubusercontent.com/60180336/187493579-0e440eca-9b7b-45d2-87c4-ae0c007aeeb9.jpg)
- You can either run the ``BenchmarkingClient\BenchmarkingManager\jar\BenchmarkingManager-1.0-SNAPSHOT.jar`` file with the respective parameters or run the ``main`` function in ``BenchmarkingClient\BenchmarkingManager\src\main\kotlin\Main.kt`` with the respective parameters. The default threads to sent queries to each version is set to 4 but can be changed in the code.


## Evaluation Latency Measurements
- ``Results\Lineplots.ipynb`` and ``Results\boxplots.ipynb`` are used for the statistical evaluation of the latency measurements. Furthermore, they create line- and boxplots. In their code you need specify the files in which the measurements are saved. 