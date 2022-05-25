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

variable "nodes" {
  type = list(string)
  default = ["cassandra-node-1", "cassandra-node-2", "cassandra-node-3"]
}