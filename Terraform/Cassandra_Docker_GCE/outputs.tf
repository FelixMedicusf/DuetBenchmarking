output "node_names_and_ip_addresses"{
        value = {
            for n, k in var.nodes : k => "${module.node[k].instance_name} -> ${module.node[k].public_ip}"
        }   
}

