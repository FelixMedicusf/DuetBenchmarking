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

variable "names_and_regions"{
  type = map(string)

  # europe-west1: Belgien, europe-west2: London, europe-west-3: FFM 
  default = {
    worker-1 = "europe-west1"
    worker-2 = "europe-west2"  
    worker-3 = "europe-west3" 
  }
}
