import com.beust.jcommander.JCommander
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Deferred
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import java.lang.Math.ceil


@OptIn(ExperimentalSerializationApi::class)
suspend fun main (vararg argv: String){

    val workloadAPath = "src\\main\\resources\\workloadA"
    val workloadCPath = "src\\main\\resources\\workloadC"
    val workloadEPath = "src\\main\\resources\\workloadE"

    println("Maximum heap size -> ${Runtime.getRuntime().maxMemory()*0.000001}")


    val args = Args()
    JCommander.newBuilder()
        .addObject(args)
        .build()
        .parse(*argv);

    println("workers --> ${args.workerIps}")

    println("nodes --> ${args.cassandraNodeIps}")

    println("Frequencies --> ${args.frequencies}")


    println("Regions --> ${args.regions}")

    println("Run --> ${args.run}")

    if(args.run)args.workload="src\\main\\resources\\workloadA_1m\\run_operations.dat"
    println("Workload --> ${args.workload}")

    var pathToTransformedOps = ""
    if(!args.run)pathToTransformedOps = "src\\main\\resources\\transformedLoads.dat"
    if(args.run)pathToTransformedOps = "src\\main\\resources\\transformedRuns.dat"

    val ca = CassandraQueries()

    var genericQueriesList: List<String>

    if(!args.run) {
        genericQueriesList = returnQueryListFromFile(args.workload, 1000000)
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
            ca::transformLoadOperations)
    }
    if(args.run){
        genericQueriesList = returnQueryListFromFile(args.workload, 1000000)
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
            ca::transformRunOperations)
    }

    genericQueriesList = listOf()

    var cassandraQueriesList = returnQueryListFromFile(pathToTransformedOps, 1000000)

    var queriesWithIds = assignIdsToQueries(cassandraQueriesList)

    cassandraQueriesList.toMutableList().clear()

    // queriesWithIds.forEach { println("${it.first} + ${it.second}") }


    // Replace numberOfWorker with args.size (number of ipAddresses equals number of nodes)
    var queriesPerWorkerWithIds = divideQueryList(args.workerIps.size, queriesWithIds)
    queriesWithIds = listOf()
    println("Current size of heap -> ${Runtime.getRuntime().totalMemory()*0.000001}")

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
        client.post("$url/api/setFirstWorkload") {
            var chunk = kotlin.math.ceil((queriesPerWorkerWithIds[index].size).toDouble()/2).toInt()
            var content = Json.encodeToString(queriesPerWorkerWithIds[index].chunked(chunk)[0])
            setBody(content)
            content = ""
        }

        client.post("$url/api/setSecondWorkload") {
            var chunk = kotlin.math.ceil((queriesPerWorkerWithIds[index].size).toDouble()/2).toInt()
            var content = Json.encodeToString(queriesPerWorkerWithIds[index].chunked(chunk)[1])
            setBody(content)
            content = ""
        }




        client.post("$url/api/setNodesAndFrequencies") {
            val content = Json.encodeToString(ipsAndFrequencies)
            setBody(content)
        }

        client.post("$url/api/setRegion"){
            val content = Json.encodeToString(args.regions[index])
            setBody(content)
        }


        val numberOfThreadsPerWorkerVM = 3
        client.get("$url/api/setThreads?threads=${numberOfThreadsPerWorkerVM}")

    }

    queriesPerWorkerWithIds = listOf()


    // Start benchmark execution of all worker nodes
    for(ip in args.workerIps) {
        val url = "http://$ip:8080"
        client.get("$url/api/startBenchmark")
    }

    val totalMeasurements = mutableListOf<Measurement>()
    runBlocking {
        val receivedFrom = arrayListOf<Int>()
        while(receivedFrom.size < args.workerIps.size) {
            // Wait for 5 minutes and then ask again for measurements from all workers. When all workers sent their
            // latency measurements leave the while loop
            delay(1500000)
            for ((index, ip) in args.workerIps.withIndex()) {
                if(index !in receivedFrom) {
                    val url = "http://$ip:8080"
                    println("Send getResults-Request to $url")
                    val response1 = client.get("$url/api/getFirstResults")
                    val response2 = client.get("$url/api/getSecondResults")
                    val response3 = client.get("$url/api/getThirdResults")
                    val response4 = client.get("$url/api/getForthResults")
                    if (response1.status == HttpStatusCode.OK && response2.status == HttpStatusCode.OK &&
                        response3.status == HttpStatusCode.OK && response4.status == HttpStatusCode.OK) {
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response1.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response2.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response3.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response4.bodyAsText())
                        receivedFrom += index
                        println("Received results from worker $index")

                    }
                }
            }
        }
    }

    println("Received all measurements from all workers!")
    // write Results to file
    if(!args.run) {
        try {
            writeMeasurementsToCsvFile("C:\\Users\\Felix Medicus\\Dokumente\\run_measurements_200_000_3t.csv", totalMeasurements, args.regions)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    if(args.run) {
        try {
            writeMeasurementsToCsvFile("C:\\Users\\Felix Medicus\\Dokumente\\run_measurements_200_000_3t.csv", totalMeasurements, args.regions)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    if(!args.run) println("Wrote all measurements to file ~/Documents/DuetBenchmarking/load_measurements.dat")
    if(args.run) println("Wrote all measurements to file ~/Documents/DuetBenchmarking/run_measurements.dat")



}
