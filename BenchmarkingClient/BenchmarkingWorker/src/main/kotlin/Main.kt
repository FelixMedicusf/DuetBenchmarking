import java.net.InetSocketAddress
import io.ktor.application.call
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
import kotlinx.serialization.encodeToString
import io.ktor.http.*
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.math.ceil



fun loadWorkload(workloadAsJson: String): List<Pair<Int, String>>{
    val workloadAsList = Json.decodeFromString<List<Pair<Int,String>>>(workloadAsJson)
    return workloadAsList
}

fun loadIpsAndFrequencies(IpsAndFrequenciesAsJson: String): List<Pair<String, Double>>{
    val IpsAndFrequenciesAsList = Json.decodeFromString<List<Pair<String, Double>>>(IpsAndFrequenciesAsJson)
    return IpsAndFrequenciesAsList
}



var id: Int = 1
var status:String = "waiting"

var workload: List<Pair<Int, String>>? = null
// var workloadA = Collections.synchronizedList(listOf<Pair<String, String>>())
// var workloadB = Collections.synchronizedList(listOf<Pair<String, String>>())
// var operationsPerWorker = 0;

var numberOfThreadsPerVersion: Int = 3
var threads = numberOfThreadsPerVersion * 2
var executor: ExecutorService = Executors.newFixedThreadPool(threads)
var socketsA = mutableListOf<InetSocketAddress>()
var socketsB = mutableListOf<InetSocketAddress>()
var managerIp = ""
var ipsAndFrequencies = mutableListOf<Pair<String,Double>>()
var benchmarkFinished = false
var region = "europe-west1"


@OptIn(ExperimentalSerializationApi::class)
fun main() {

    // Take as program args
    // Default values, can also be set by Benchmarking Manager
    val ipAddresses : Array<String> = arrayOf("34.77.218.161","35.189.111.242","34.159.113.65")
    var queryIntensity: Array<Double> = arrayOf(3.3,3.3,1.0)


    var ipIndexAndOccurrence = mutableMapOf<Int, Double>()

    for(index in ipAddresses.indices){
        ipIndexAndOccurrence[index] = queryIntensity[index]
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

        println("Maximum heap size -> ${Runtime.getRuntime().maxMemory()*0.000001}")

       routing {
           get("api/getStatus"){
               log.info("Status requested")
               managerIp = call.request.origin.remoteHost
               log.info("Responded $status to $managerIp")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText(status, ContentType.Text.Plain)
               managerIp = call.request.origin.remoteHost

           }

           post("api/setRegion"){
               val content = call.receiveText()
               region = Json.decodeFromString<String>(content)
               log.info("Request to change region (datacenter) worker-$id should send requests to. ($region)")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText("Regions set", ContentType.Text.Plain)
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
                   ipIndexAndOccurrence[index] = queryIntensity[index]
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
               log.info("Request to set number of Threads per Version to ${call.parameters["threads"]}")
               numberOfThreadsPerVersion = call.parameters["threads"]?.toInt() ?: 1
               threads = numberOfThreadsPerVersion*2
               log.info("Set total number of threads to $threads and threads per version to $numberOfThreadsPerVersion")
               call.response.header(name="Access-Control-Allow-Origin", value="*")
               call.respondText(text = "OK", status=HttpStatusCode.OK, contentType=ContentType.Application.Any)

           }

           post("api/setFirstWorkload"){
               log.info("Request to set first part of the Workload")
               managerIp = call.request.origin.remoteHost
               var content = call.receiveText()
               workload = loadWorkload(content)
               content = ""

               log.info("Received first Part of Workload with ${workload?.size} queries from $managerIp")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText(text ="OK", status = HttpStatusCode.OK, contentType =  ContentType.Application.Any)
           }
           post("api/setSecondWorkload"){
               log.info("Request to set second part of the Workload")
               managerIp = call.request.origin.remoteHost
               var content = call.receiveText()
               workload = workload?.plus(loadWorkload(content))
               content = ""

               log.info("Received second Part of Workload queries from $managerIp and appended it to the first part. Total size of workload:  ${workload?.size}")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText(text ="OK", status = HttpStatusCode.OK, contentType =  ContentType.Application.Any)
           }

           get("api/getFirstResults"){
                if (benchmarkFinished){
                    log.info("Received Request for results")
                    val responseBody = Json.encodeToString((latencies.chunked(ceil((latencies.size).toDouble()/4).toInt()))[0])
                    call.response.header("Access-Control-Allow-Origin", "*")
                    call.respondText(responseBody, ContentType.Application.Json)
                    workload = null
                    log.info("First part of results sent to $managerIp")

                }else {
                    log.info("Requested Results before conducting Benchmark")
                    call.response.header("Access-Control-Allow-Origin", "*")
                    call.respondText(text="Benchmark hasn't finished yet. No results available yet",status=HttpStatusCode.NotFound, contentType=ContentType.Application.Any)
                }
           }

           get("api/getSecondResults"){
               if (benchmarkFinished){
                   val responseBody = Json.encodeToString((latencies.chunked(ceil((latencies.size).toDouble()/4).toInt()))[1])
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText(text = responseBody, status=HttpStatusCode.OK, contentType = ContentType.Application.Json)
                   workload = null
                   log.info("Second part of results sent to $managerIp")
               }else {
                   log.info("Requested Results before conducting Benchmark")
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText(text="Benchmark hasn't finished yet. No results available yet",status=HttpStatusCode.NotFound, contentType=ContentType.Application.Any)
               }
           }
           get("api/getThirdResults"){
               if (benchmarkFinished){
                   val responseBody = Json.encodeToString((latencies.chunked(ceil((latencies.size).toDouble()/4).toInt()))[2])
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText(text = responseBody, status=HttpStatusCode.OK, contentType = ContentType.Application.Json)
                   workload = null
                   log.info("Third part of results sent to $managerIp")
               }else {
                   log.info("Requested Results before conducting Benchmark")
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText(text="Benchmark hasn't finished yet. No results available yet",status=HttpStatusCode.NotFound, contentType=ContentType.Application.Any)
               }
           }
           get("api/getForthResults"){
               if (benchmarkFinished){
                   val responseBody = Json.encodeToString((latencies.chunked(ceil((latencies.size).toDouble()/4).toInt()))[3])
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText(text = responseBody, status=HttpStatusCode.OK, contentType = ContentType.Application.Json)
                   workload = null
                   log.info("Forth part of results sent to $managerIp")
               }else {
                   log.info("Requested Results before conducting Benchmark")
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText(text="Benchmark hasn't finished yet. No results available yet",status=HttpStatusCode.NotFound, contentType=ContentType.Application.Any)
               }
           }
           get("api/startBenchmark"){
               latencies.clear()
               benchmarkFinished = false
               val latch = CountDownLatch(1)

               if (workload == null)log.info("Attempt to start Benchmark without prior set Workload. Abort benchmark start!")

               else {
                   log.info("Start benchmark run...")
                   status = "running"
                   executor = Executors.newFixedThreadPool(threads)
                   for (i in 1 .. numberOfThreadsPerVersion){
                       
                        val workloadForThread = divideListForThreads(workload!!)?.get(i-1) ?: emptyList()
                        val sutList = getSutList(
                            ipIndexAndOccurrence,
                            workloadForThread.size
                            )

                        val workerA = WorkerThread("w${id}-vA", socketsA[id-1], sutList,
                            workloadForThread, region, latch, )

                        val workerB = WorkerThread("w${id}-vB", socketsB[id-1], sutList,
                           workloadForThread, region, latch, )

                       // Create Threadpool if more than 1 Thread per Version is necessary.
                       executor.execute(workerA)
                       executor.execute(workerB)

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
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText(text = "Benchmark started!", status = HttpStatusCode.OK, contentType = ContentType.Application.Json)

               }
           }
           get("api/clear"){
               workload = null
               status = "waiting"
               log.info("Cleared Workload")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText(text="Workload cleared", status = HttpStatusCode.OK, contentType =  ContentType.Application.Any)
           }
       }
    }.start(wait=true)


}

fun divideListForThreads(workload :List<Pair<Int, String>>):List<List<Pair<Int, String>>>?{

    val chunkSize = ceil(workload.size.toDouble()/numberOfThreadsPerVersion).toInt()

    return workload.chunked(chunkSize)
}





