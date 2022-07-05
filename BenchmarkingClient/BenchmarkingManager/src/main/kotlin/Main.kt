import com.beust.jcommander.JCommander
import com.beust.jcommander.Parameter
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Deferred
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking


suspend fun main (vararg argv: String){

    val workloadAPath = "src\\main\\resources\\workloadA"
    val workloadCPath = "src\\main\\resources\\workloadC"
    val workloadEPath = "src\\main\\resources\\workloadE"


    val args = Args()
    JCommander.newBuilder()
        .addObject(args)
        .build()
        .parse(*argv);

    println("workers --> ${args.workerIps}")

    println("nodes --> ${args.cassandraNodeIps}")

    println("Frequencies --> ${args.frequencies}")

    println("Workload --> ${args.workload}")

    println("Regions --> ${args.regions}")

    println("Run --> ${args.run}")

    if(args.run)args.workload="src\\main\\resources\\workloadA_10000\\run_operations_10000.dat"

    var pathToTransformedOps = ""
    if(!args.run)pathToTransformedOps = "src\\main\\resources\\workloadA_10000\\cassandra_load_operations_10000.dat"
    if(args.run)pathToTransformedOps = "src\\main\\resources\\workloadA_10000\\cassandra_run_operations_10000.dat"

    val ca = CassandraQueries()

    var genericQueriesList: List<String>

    if(!args.run) {
        genericQueriesList = returnQueryListFromFile(args.workload)
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
            ca::transformLoadOperations)
    }
    if(args.run){
        genericQueriesList = returnQueryListFromFile(args.workload)
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
            ca::transformRunOperations)
    }

    var cassandraQueriesList = returnQueryListFromFile(pathToTransformedOps)

    var queriesWithIds = assignIdsToQueries(cassandraQueriesList)

    // Replace numberOfWorker with args.size (number of ipAddresses equals number of nodes)
    var queriesPerWorkerWithIds = divideQueryList(3, queriesWithIds)

    // Carry out following operations for all workers
    val client = HttpClient(CIO){
        // disable request timeout cause benchmark takes time
        engine {
            requestTimeout = 0
        }
    }

    var ipsAndFrequencies = mutableListOf<Pair<String, Double>>()

    for (i in 0 until args.cassandraNodeIps.size){
        ipsAndFrequencies.add(Pair(args.cassandraNodeIps[i], args.frequencies[i]))
    }

    for((index, ip) in args.workerIps.withIndex()) {

        val url = "http://$ip:8080"

        client.get("$url/api/setId?id=${index+1}") {

        }

        var workerId = client.get("$url/api/getId").body<String>()

        // Send every #{node}th transformed query to the worker
        client.post("$url/api/setWorkload") {
            val content = Json.encodeToString(queriesPerWorkerWithIds[index])
            setBody(content)
        }


        client.post("$url/api/setNodesAndFrequencies") {
            val content = Json.encodeToString(ipsAndFrequencies)
            setBody(content)
        }

        // Needs to be an even number
        val numberOfThreadsPerWorkerNode = 4
        client.get("$url/api/setThreads?threads=${numberOfThreadsPerWorkerNode}")

    }
        var totalMeasurements = mutableListOf<Triple<String, Long, Long>>()
        var responses = mutableListOf<Deferred<String>>()

        // Start Benchmark ("simultaneously")
        runBlocking {
            for(ip in args.workerIps) {
                val url = "http://$ip:8080"
                val response = async {client.get("$url/api/startBenchmark").bodyAsText()}
                responses.add(response)
            }
            for(response in responses){
                totalMeasurements += (Json.decodeFromString<List<Triple<String, Long, Long>>>(response.await()))

            }
        }

    println("Received all measurements from all workers!")

    // write Results to file
    try {
    writeResultsToFile("C:\\Users\\Felix Medicus\\Dokumente\\measurements.dat", totalMeasurements)
    writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
    }catch(e: java.lang.Exception){

    }

    println("Wrote all measurements to file ~/Documents/DuetBenchmarking/measurements.dat")



}
