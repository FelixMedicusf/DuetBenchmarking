gcloud compute ssh --zone europe-west1-b cassandra-node-1 -- "sudo docker cp cassandra-container-1a:/etc/cassandra/cassandra.yaml ~/cassandra_config.yaml"
gcloud compute scp --zone europe-west1-b --recurse cassandra-node-1:~/cassandra_config.yaml ~/Documents/cassandra_config.yaml