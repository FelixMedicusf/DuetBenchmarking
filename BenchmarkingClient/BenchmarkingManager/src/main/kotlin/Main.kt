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
import java.nio.file.Path
import java.nio.file.Paths


@OptIn(ExperimentalSerializationApi::class)
suspend fun main (vararg argv: String){


    println("Maximum heap size -> ${Runtime.getRuntime().maxMemory()*0.000001}")


    val args = Args()
    JCommander.newBuilder()
        .addObject(args)
        .build()
        .parse(*argv);

    val numberOfThreadsPerWorkerVM = 4
    
    println("Workload --> ${args.workload}")

    var pathToTransformedOps = ""
    if(!args.run)pathToTransformedOps = "src\\main\\resources\\transformedLoads.dat"
    if(args.run)pathToTransformedOps = "src\\main\\resources\\transformedRuns.dat"

    val ca = CassandraQueries()

    var genericQueriesList: List<String>

    if(!args.run) {
        genericQueriesList = returnQueryListFromFile(args.workload, 1_000_000)
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
            ca::transformLoadOperations)
    }
    if(args.run){
        genericQueriesList = returnQueryListFromFile(args.workload, 1_000_000)
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
            ca::transformRunOperations)
    }

    genericQueriesList = listOf()

    var cassandraQueriesList = returnQueryListFromFile(pathToTransformedOps, 1_000_000)

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

    var ipsAndFrequencies = mutableListOf<String>()

    for (i in 0 until args.cassandraNodeIps.size){
        ipsAndFrequencies.add(args.cassandraNodeIps[i])
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




        client.post("$url/api/setNodes") {
            val content = Json.encodeToString(ipsAndFrequencies)
            setBody(content)
        }

        client.post("$url/api/setRegion"){
            val content = Json.encodeToString(args.regions[index])
            setBody(content)
        }



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
            // Wait for 20 minutes and then ask again for measurements from all workers. When all workers sent their
            // latency measurements leaves the while loop
            delay(1_200_000)
            for ((index, ip) in args.workerIps.withIndex()) {
                if(index !in receivedFrom) {
                    val url = "http://$ip:8080"
                    println("Send getResults-Request to $url")
                    val response1 = client.get("$url/api/getResultsFirst")
                    val response2 = client.get("$url/api/getResultsSecond")

                    if (response1.status == HttpStatusCode.OK && response2.status == HttpStatusCode.OK) {
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response1.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response2.bodyAsText())
                        receivedFrom += index
                        println("Received results from worker $index")

                    }
                }
            }
        }
    }

    println("Received all measurements from all workers!")

    // write Results to file
    val cwd = System.getProperty("user.dir")
    var path = ""


    if(!args.run) {
        try {
            path = Paths.get(cwd, "load_measurements.csv").toString()
            writeMeasurementsToCsvFile(path.toString(), totalMeasurements, args.regions)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    if(args.run) {
        try {
            path = Paths.get(cwd, "run_measurements.csv").toString()
            writeMeasurementsToCsvFile(path.toString(), totalMeasurements, args.regions)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    println("Wrote all measurements to file $path")

}
