import com.beust.jcommander.JCommander
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
import kotlinx.coroutines.delay
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
        val numberOfThreadsPerWorkerVM = 2
        client.get("$url/api/setThreads?threads=${numberOfThreadsPerWorkerVM}")

    }
        var totalMeasurements = mutableListOf<Measurement>()
        var responses = mutableListOf<Deferred<String>>()

        // Start Benchmark ("simultaneously")
    /*
        runBlocking {
            for(ip in args.workerIps) {
                val url = "http://$ip:8080"
                async {client.get("$url/api/startBenchmark")}
            }
        }

     */
    for(ip in args.workerIps) {
        val url = "http://$ip:8080"
        client.get("$url/api/startBenchmark")
    }


    runBlocking {
        var receivedFrom = arrayListOf<Int>()
        while(totalMeasurements.size<queriesWithIds.size*2) {
            for ((index, ip) in args.workerIps.withIndex()) {
                val url = "http://$ip:8080"
                // Ask every 100 seconds for results
                delay(100000)
                val response = client.get("$url/api/getResults").bodyAsText()
                if(response.length > 100 && index !in receivedFrom){
                    totalMeasurements+= Json.decodeFromString<List<Measurement>>(response)
                    receivedFrom+=index
                }
            }
        }
    }

    println("Received all measurements from all workers!")
    // write Results to file
    if(!args.run) {
        try {
            writeMeasurementsToFile("C:\\Users\\Felix Medicus\\Dokumente\\load_measurements.dat", totalMeasurements)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    if(args.run) {
        try {
            writeMeasurementsToFile("C:\\Users\\Felix Medicus\\Dokumente\\run_measurements.dat", totalMeasurements)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    println("Wrote all measurements to file ~/Documents/DuetBenchmarking/measurements.dat")



}
