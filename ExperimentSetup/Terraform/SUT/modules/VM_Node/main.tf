resource "google_compute_address" "static_ip" {
  name = "${var.instance-name}-ipv4-address"
  region = trim(var.zone, "-abcd")
}

resource "google_compute_instance" "cassandra" {
  name = var.instance-name
  machine_type = "e2-medium"
  zone = var.zone
  tags = ["allow-traffic", "allow-ssh"]

  depends_on=[google_compute_address.static_ip]
  boot_disk {
    initialize_params {
      image="ubuntu-os-pro-cloud/ubuntu-pro-2004-lts"
    }
  }

  network_interface {
    network=var.network
    access_config {
      nat_ip = google_compute_address.static_ip.address
      #ephemeral(flüchtig)
    }
  }

  metadata = {
    startup-script=<<SCRIPT
    curl -sSL https://get.docker.com/ | sh
    SCRIPT
  }
}
