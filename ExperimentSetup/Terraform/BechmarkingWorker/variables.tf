variable "project-id" {
  type = string 
  default = "first-setup-gcp"
}

variable "region" {
  type = string 
  default = "europe-west1"
}

variable "network-name" {
  type = string 
  default = "worker-network"
}

variable "names_and_zones"{
  type = map(string)

  # europe-west1: Belgien, europe-west2: London, europe-west-3: FFM 
  default = {
    worker-1 = "europe-west1-c"
    worker-2 = "europe-west2-c"  
    worker-3 = "europe-west3-c" 
  }
}
