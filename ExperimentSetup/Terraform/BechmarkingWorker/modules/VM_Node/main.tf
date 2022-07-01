resource "google_compute_address" "static_ip" {
  name = "${var.instance-name}-ipv4-address"
  region = var.region
}

resource "google_compute_instance" "worker" {
  name = "${var.instance-name}"
  machine_type="e2-medium"
  zone = "${var.region}-b"
  
  tags = ["allow-tcp", "allow-ssh"]

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
    #! /bin/bash
    sudo apt update 
    yes | sudo apt install default-jdk
    sudo apt update 
    yes | sudo apt install default-jre   
    # Create symbolic link (python -> python2.7)
    sudo ln -s /usr/bin/python2.7 /usr/bin/python 
    SCRIPT

  }
}
