provider "google" {
  project = var.project-id
  region = var.region
  zone = "${var.region}-b"
}

provider "tls" {

}

resource "tls_private_key" "ssh" {
  algorithm = "RSA"
  rsa_bits = 4096
}

resource "local_file" "ssh_private_key_pem" {
  content         = tls_private_key.ssh.private_key_pem
  filename        = ".ssh/google_compute_engine"
  file_permission = "0600" 
}

resource "google_compute_address" "static_ip" {
  name = "cassandra"
}

data "google_client_openid_userinfo" "me" {}

resource "google_compute_instance" "cassandra" {
  name = "${var.instance-name}-1"
  machine_type="e2-medium"

  tags = ["allow-traffic", "allow-ssh"]


  boot_disk {
    initialize_params {
      image="ubuntu-os-pro-cloud/ubuntu-pro-2004-lts"
    }
  }

  network_interface {
    network=google_compute_network.cassandra_network.self_link
    access_config {
      nat_ip = google_compute_address.static_ip.address
      #ephemeral(flüchtig)
    }
  }

  metadata = {
    ssh-keys = "${split("@", data.google_client_openid_userinfo.me.email)[0]}:${tls_private_key.ssh.public_key_openssh}", 
    startup-script=<<SCRIPT
    curl -sSL https://get.docker.com/ | sh

    # --name -> name Container "cassandra-instance-1-container"
    # --network -> attach container to created network ("cassandra-network")
    # -d -> run detachted (Run in background)
    # -p 9042:9042 -p 7000:7000 -> expose ports <host-port>:containerPort
    #   port 9042 for client interaction & port 7000 for node communication
    # -v cassandraData:/data -> share /data directory with cassandra folder on host
    # Set cassandra.conf values:
    # -Dcassandra.config=/path/to/cassandra.yaml
    # Must create Keyspace in Cassandra to interact with YCSB
    # CREATE KEYSPACE usertable WITH replication = {'class' :'SimpleStrategy', 'replication_factor' : 1};
    
    
    # cd ~ && echo "echo "CREATE KEYSPACE usertable WITH replication = {'class' :'SimpleStrategy', 'replication_factor' : 1};" | cqlsh" > containerScript.sh
    
    # sudo docker network create cassandra-network
    # sudo docker run --name cassandra-instance-1-container --network cassandra-network -v /docker/cassandra/instance-1:/var/lib/cassandra -p 9042:9042 -p 7000:7000 -d cassandra
    # sudo docker run --name cassandra-instance-2-container -v /docker/cassandra/instance-2:/var/lib/cassandra -p 9043:9042 -p 7001:7000 -d cassandra

    # Creating Keyspace "usertable" as required by YCSB
    # sudo docker exec cassandra-instance-1-container ~/containerScript.sh
    # sudo docker exec cassandra-instance-1-container bin/sh -c "echo 'USE usertable' | cqlsh"
    SCRIPT
    
  }
}


resource "google_compute_network" "cassandra_network" {
  name = var.network-name
  auto_create_subnetworks = "true"
}

resource "google_compute_firewall" "cassandra_firewall_ingress" {
  name="allow-ingress-cassandra"
  network=google_compute_network.cassandra_network.self_link
  source_ranges = ["0.0.0.0/0"]
  
  target_tags=["allow-traffic"]

  depends_on = [google_compute_network.cassandra_network]

  allow {
    protocol = "tcp"
    ports = [
      "80",   #HTTP
      "8080", #HTTP
      "7000", #Cassandra-Cluster Communication
      "7001", #Cassandra-Cluster Communication with enabled SSL
      "9042", #Cassandra native protocol clients 
    ]
  }
}

resource "google_compute_firewall" "cassandra_firewall_egresss" {
  name="allow-egress-cassandra"
  network=google_compute_network.cassandra_network.self_link
  destination_ranges = ["0.0.0.0/0"]
  direction = "EGRESS"
  target_tags=["allow-traffic"]

  depends_on = [google_compute_network.cassandra_network]

  allow {
    protocol = "tcp"
    ports = [
      "80",   #HTTP
      "8080", #HTTP
      "7000", #Cassandra-Cluster Communication
      "7001", #Cassandra-Cluster Communication with enabled SSL
      "9042", #Cassandra native protocol clients 
    ]
  }
}

resource "google_compute_firewall" "allow_ssh" {
  name          = "allow-ssh-cassandra"
  network       = google_compute_network.cassandra_network.self_link
  target_tags   = ["allow-ssh"] // this targets our tagged VM
  source_ranges = ["0.0.0.0/0"] // 0.0.0.0 refers to all IPv4 addresses 

  depends_on = [google_compute_network.cassandra_network]

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}