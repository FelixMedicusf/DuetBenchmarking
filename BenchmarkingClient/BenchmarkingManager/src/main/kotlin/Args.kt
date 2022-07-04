import com.beust.jcommander.Parameter


class Args {

    @Parameter(description = "Files")
    var files: List<String> = mutableListOf()

    @Parameter(names = ["-n", "-nodes"], description = "Array of Cassandra Node Ip-Addresses")
    var cassandraNodeIps: List<String> = mutableListOf()

    @Parameter(names = ["-w", "-workers"], description = "Array of Worker Ip-Addresses")
    var workerIps: List<String> = mutableListOf()

    @Parameter(names = ["-l", "-length"], description = "Total length of operations")
    var operations = 0

    @Parameter(names = ["-f", "-frequencies"], description = "Array of Frequencies in which nodes should be queried")
    var frequencies: List<Double> = mutableListOf(1.0,1.0,1.0)

    @Parameter(names = ["-r", "-regions"], description = "Array of regions in which nodes are deployed")
    var regions: List<String> = mutableListOf()

    @Parameter(names = ["-wl", "-workload"], description = "Path to the workload")
    var workload = ""
}