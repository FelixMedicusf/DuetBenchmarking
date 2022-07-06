import java.net.InetSocketAddress
import io.ktor.application.call
import io.ktor.http.ContentType
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.features.*
import io.ktor.request.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import WorkerThread
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.util.Identity.encode
import kotlinx.serialization.encodeToString
import io.ktor.client.engine.cio.*
import java.util.Collections
import kotlin.math.ceil

fun loadWorkload(workloadAsJson: String): List<Pair<String, String>>{
    val workloadAsList = Json.decodeFromString<List<Pair<String,String>>>(workloadAsJson)
    return workloadAsList
}

fun loadIpsAndFrequencies(IpsAndFrequenciesAsJson: String): List<Pair<String, Double>>{
    val IpsAndFrequenciesAsList = Json.decodeFromString<List<Pair<String, Double>>>(IpsAndFrequenciesAsJson)
    return IpsAndFrequenciesAsList
}



var id: Int = 1
var status:String = "waiting"

var workload: List<Pair<String, String>>? = null
// var workloadA = Collections.synchronizedList(listOf<Pair<String, String>>())
// var workloadB = Collections.synchronizedList(listOf<Pair<String, String>>())
// var operationsPerWorker = 0;

var numberOfThreadsPerVersion: Int = 1
var threads = numberOfThreadsPerVersion * 2
var executor: ExecutorService = Executors.newFixedThreadPool(threads)
var socketsA = mutableListOf<InetSocketAddress>()
var socketsB = mutableListOf<InetSocketAddress>()
var managerIp = ""
var ipsAndFrequencies = mutableListOf<Pair<String,Double>>()
var benchmarkFinished = false


fun main(args: Array<String>) {

    // Take as program args
    // Default values, can also be set by Benchmarking Manager
    val ipAddresses : Array<String> = arrayOf("34.77.218.161","35.189.111.242","34.159.113.65")
    var queryIntensity: Array<Double> = arrayOf(3.3,3.3,1.0)
    val datacenters = listOf<String>("europe-west1", "europe-west2", "europe-west3")

    var ipIndexAndOccurrence = mutableMapOf<Int, Double>()

    for(index in ipAddresses.indices){
        ipIndexAndOccurrence.put(index, queryIntensity[index])
    }

    try {
        for (address in ipAddresses) {
            val socketA = InetSocketAddress(address, 9045)
            val socketB = InetSocketAddress(address, 9050)
            socketsA.add(socketA)
            socketsB.add(socketB)
        }

    }catch(e: java.lang.Exception){
        print("Unable to establish connection to one of the Cluster's Nodes!")
        e.printStackTrace()
    }


    embeddedServer(Netty, port = 8080){
       routing {
           get("api/getStatus"){
               log.info("Status requested")
               managerIp = call.request.origin.remoteHost
               log.info("Responded $status to $managerIp")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText(status, ContentType.Text.Plain)
               managerIp = call.request.origin.remoteHost

           }


           post("api/setNodesAndFrequencies"){
               log.info("Request to change Nodes and Frequencies")
               val content = call.receiveText()
               ipsAndFrequencies = loadIpsAndFrequencies(content).toMutableList()
               socketsA.clear()
               socketsB.clear()

               for (ipAndFrequency in ipsAndFrequencies) {
                   val socketA = InetSocketAddress(ipAndFrequency.first, 9045)
                   val socketB = InetSocketAddress(ipAndFrequency.first, 9050)
                   socketsA.add(socketA)
                   socketsB.add(socketB)
               }


               queryIntensity = ipsAndFrequencies.map{it.second}.toTypedArray()

               for(index in ipsAndFrequencies.indices){
                   ipIndexAndOccurrence.put(index, queryIntensity[index])
               }
               log.info("Node Ips and Frequencies set")
               call.response.header("Access-Control-Allow-Origin", "*")

               call.respondText("OK", ContentType.Application.Any)
           }


           get("api/setId"){
               managerIp = call.request.origin.remoteHost
               log.info("Request from $managerIp to set Worker id to ${call.parameters["id"]}")
               id = call.parameters["id"]?.toInt() ?: id
               call.respondText("OK", ContentType.Application.Any)
           }

           get("api/getId"){
               log.info("Request from $managerIp to get Id of Worker")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText(id.toString(), ContentType.Application.Any)
           }

           get("api/setThreads"){
               log.info("Request to set total number of Threads (for Version A and B) to ${call.parameters["threads"]}")
               threads = call.parameters["threads"]?.toInt() ?: 2
               numberOfThreadsPerVersion = (threads/2)
               log.info("Set total number of threads to $threads and threads per version to $numberOfThreadsPerVersion")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText("Ok", ContentType.Application.Any)

           }

           post("api/setWorkload"){
               log.info("Request to set the Workload")
               managerIp = call.request.origin.remoteHost
               val content = call.receiveText()
               workload = loadWorkload(content)
               workloadA = loadWorkload(content)
               workloadB = loadWorkload(content)
               log.info("Received Workload with ${workload?.size} queries from $managerIp")
               // operationsPerWorker = (workload?.size ?: 0) / numberOfThreadsPerVersion
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText("OK", ContentType.Application.Any)
           }

           get("api/getResults"){
                if (benchmarkFinished){
                    log.info("Requested Benchmarking results by $managerIp")
                    val responseBody = Json.encodeToString(latencies)
                    call.response.header("Access-Control-Allow-Origin", "*")
                    call.respondText(responseBody, ContentType.Application.Json)
                    benchmarkFinished = !benchmarkFinished
                    workload = null
                }else {
                    log.info("Requested Results before conducting Benchmark")
                    call.response.header("Access-Control-Allow-Origin", "*")
                    call.respondText("Benchmark hasn't finished yet. No results available yet", ContentType.Application.Any)
                }
           }
           get("api/startBenchmark"){
               latencies.clear()
               val latch = CountDownLatch(1)

               if (workload == null)log.info("Attempt to start Benchmark without prior set Workload. Abort benchmark start!")
               else {
                   log.info("Start benchmark run...")
                   status = "running"
                   executor = Executors.newFixedThreadPool(threads)
                   for (i in 1 .. numberOfThreadsPerVersion){
                        val workerA = WorkerThread("Thread-${i}a-Worker-$id", socketsA, getSutList(
                            ipIndexAndOccurrence,
                            ((workload?.size)) ?: 0
                        ),
                            divideListForThreads(workload!!)?.get(i-1) ?: emptyList() ,datacenters, latch, )

                       // Create Threadpool if more than 1 Thread per Version is necessary.
                       executor.execute(workerA)
                      // workerA.start()

                       val workerB = WorkerThread("Thread-${i}b-Worker-$id", socketsB, getSutList(
                           ipIndexAndOccurrence,
                           workload?.size ?: 0
                       ), workload!!, datacenters, latch, )

                       executor.execute(workerB)
                       //workerB.start()
                   }

                   GlobalScope.launch {
                       executor.shutdown()
                       latch.countDown()
                       executor.awaitTermination(1, TimeUnit.HOURS)
                       log.info("Worker Done")
                       benchmarkFinished = true
                       status = "waiting"
                       log.info("Length of Measurements for both Threads: ${latencies.size}")
                   }

                   log.info("Benchmark started")
                   executor.awaitTermination(1, TimeUnit.HOURS)
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText("Benchmark started!", ContentType.Application.Json)


               }
           }
           get("api/clear"){
               workload = null
               status = "waiting"
               log.info("Cleared Workload")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText("OK", ContentType.Application.Any)
           }
       }
    }.start(wait=true)


}

fun divideListForThreads(workload :List<Pair<String, String>>):List<List<Pair<String, String>>>?{
    var chunkedList = emptyList<List<Pair<String, String>>>()
    if (numberOfThreadsPerVersion == 1){
        chunkedList = workload.chunked(workload.size)
    }
    if (numberOfThreadsPerVersion == 2){
        var middleIndex = ceil(workload.size.toDouble()/2).toInt()
        chunkedList = workload.chunked(middleIndex)
    }

    return chunkedList


}





