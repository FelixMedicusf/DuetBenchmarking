output "nodes_ips_zones"{
        value = {
            for n, k in var.names_and_zones : n => " ${module.node[n].public_ip} --> ${module.node[n].zone}"
        }   
}

