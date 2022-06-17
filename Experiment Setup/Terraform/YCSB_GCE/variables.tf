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
  default = "ycsb-instance"
}

variable "network-name" {
  type = string 
  default = "ycsb-network"
}