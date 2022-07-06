provider "google" {
  project = var.project-id
  region = var.region
  zone = "${var.region}-b"
}

module "node" {
  source = "./modules/VM_Node"
  for_each=var.names_and_zones
  network = google_compute_network.cassandra_network.self_link
  instance-name = each.key
  zone = each.value
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
      "7005", #Internode Communication Cluster A
      "7010", #Internode Communication Cluster B
      "9045", #Client Communication Cluster A
      "9050"  #Client Communication Cluster B
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
      "7005", #Internode Communication Cluster A
      "7010", #Internode Communication Cluster B 
      "9045", #Client Communication Cluster A
      "9050"  #Client Communication Cluster B
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