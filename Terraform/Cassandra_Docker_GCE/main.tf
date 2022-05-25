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