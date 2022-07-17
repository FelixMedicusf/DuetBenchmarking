variable "project-id" {
  type = string 
  default = "first-setup-gcp"
}

variable "region" {
  type = string 
  default = "europe-west1"
}

variable "instance-name" {
  type = string
  default = "cassandra-instance"
}

variable "network-name" {
  type = string 
  default = "cassandra-network"
}

variable "names_and_zones"{
  type = map(string)

  # europe-west1: Belgien, europe-west2: London, europe-west-3: FFM 
  default = {
    cassandra-node-1 = "europe-west1-b"
    cassandra-node-2 = "europe-west2-b"  
    cassandra-node-3 = "europe-west3-b" 
  }
}



