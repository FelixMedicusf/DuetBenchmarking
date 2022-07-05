output "nodes_ips_zones"{
        value = {
            for n, k in var.names_and_regions : k => "${module.node[n].instance_name} -> ${module.node[n].public_ip} -> ${module.node[n].zone}"
        }   
}

