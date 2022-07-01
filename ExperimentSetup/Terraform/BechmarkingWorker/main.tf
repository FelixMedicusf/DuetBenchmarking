provider "google" {
  project = var.project-id
  region = var.region
  zone = "${var.region}-b"
}
module "node" {
  source = "./modules/VM_Node"
  for_each=var.names_and_regions
  network = google_compute_network.worker_network.self_link
  instance-name = each.key
  region = each.value
  depends_on=[google_compute_network.worker_network]

}
# Create a VPC 
resource "google_compute_network" "worker_network" {
   name = var.network-name
   auto_create_subnetworks = "true"
}

resource "google_compute_firewall" "worker_firewall_ingress" {
  name="allow-ingress-worker"
  network=google_compute_network.worker_network.self_link
  source_ranges = ["0.0.0.0/0"]
  
  target_tags=["allow-tcp"]

  allow {
    protocol = "tcp"
    ports = [
      "80", #HTTP
      "8080", #HTTP
      "7000", #Cassandra-Cluster Communication
      "7001", #Cassandra-Cluster Communication with enabled SSL
      "9042", #Cassandra native protocol clients 
    ]
  }
}
resource "google_compute_firewall" "worker_firewall_egress" {
  name="allow-egress-worker"
  network=google_compute_network.worker_network.self_link
  destination_ranges = ["0.0.0.0/0"]
  direction = "EGRESS"
  target_tags=["allow-tcp"]

  allow {
    protocol = "tcp"
    ports = [
      "80", #HTTP
      "8080", #HTTP
      "7000", #Cassandra-Cluster Communication
      "7001", #Cassandra-Cluster Communication with enabled SSL
      "9042", #Cassandra native protocol clients 
    ]
  }
}

resource "google_compute_firewall" "allow_ssh" {
  name          = "allow-ssh-worker"
  network       = google_compute_network.worker_network.self_link
  target_tags   = ["allow-ssh"] // this targets our tagged VM
  source_ranges = ["0.0.0.0/0"] // 0.0.0.0 refers to all IPv4 addresses 

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}