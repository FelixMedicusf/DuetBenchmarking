provider "google" {
  project = var.project-id
  region = var.region
  zone = "${var.region}-b"
}

module "node" {
  source = "./modules/VM_Node"
  for_each=toset(var.nodes)
  network = google_compute_network.cassandra_network.self_link
  instance-name = each.value
  instance-number = each.key
  depends_on=[google_compute_network.cassandra_network]

}

resource "google_compute_network" "cassandra_network" {
  name = var.network-name
  auto_create_subnetworks = "true"
}

resource "google_compute_firewall" "cassandra_firewall_ingress" {
  name="allow-ingress-cassandra"
  network=google_compute_network.cassandra_network.self_link
  source_ranges = ["0.0.0.0/0"]

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
  source_ranges = ["0.0.0.0/0"] // 0.0.0.0 refers to all IPv4 addresses 

  depends_on = [google_compute_network.cassandra_network]

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }
}