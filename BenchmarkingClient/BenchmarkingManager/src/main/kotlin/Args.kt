import com.beust.jcommander.Parameter


class Args {

    @Parameter(description = "Files")
    var files: List<String> = mutableListOf()

    @Parameter(names = ["-n", "-nodes"], description = "Array of Cassandra Node Ip-Addresses", echoInput = true)
    var cassandraNodeIps: List<String> = mutableListOf()

    @Parameter(names = ["-w", "-workers"], description = "Array of Worker Ip-Addresses", echoInput = true)
    var workerIps: List<String> = mutableListOf()

    @Parameter(names = ["-f", "-frequencies"], description = "Array of Frequencies in which nodes should be queried", echoInput = true)
    var frequencies: List<Double> = mutableListOf(1.0,1.0,1.0)

    @Parameter(names = ["-r", "-regions"], description = "Array of regions in which nodes are deployed", echoInput = true)
    var regions: List<String> = mutableListOf()

    @Parameter(names = ["-wl", "-workload"], description = "Path to the workload", echoInput = true)
    var workload = "src\\main\\resources\\workloadC_1.25m\\load_operations.dat"

    @Parameter(names = ["-run"], description = "Set -run if it is a run Benchmark", echoInput = true)
    var run:Boolean = false
}