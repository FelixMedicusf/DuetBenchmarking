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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

fun loadWorkload(workloadAsJson: String): List<Pair<String, String>>{
    val workloadAsList = Json.decodeFromString<List<Pair<String,String>>>(workloadAsJson)
    return workloadAsList
}



var id: Int = 1
var status:String = "waiting"
var workload: List<Pair<String, String>>? = null
var numberOfThreadsPerVersion: Int = 1
var threads = numberOfThreadsPerVersion * 2
var executor: ExecutorService = Executors.newFixedThreadPool(threads)
val socketsA = mutableListOf<InetSocketAddress>()
val socketsB = mutableListOf<InetSocketAddress>()

var ipIndexAndOccurrence = mutableMapOf<Int, Double>()

fun main(args: Array<String>) {

    // Take as program args
    val ipAddresses : Array<String> = arrayOf("35.187.119.157","35.195.248.109","34.77.86.239")
    val queryIntensity: Array<Double> = arrayOf(3.3,3.3,1.0)

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
               log.info("Responded $status")
               call.response.header("Access-Control-Allow-Origin", "*")
               call.respondText(status, ContentType.Text.Plain)
           }

           post("api/setWorkload"){
               val content = call.receiveText()
               workload = loadWorkload(content)
               log.info("Received Workload with ${workload?.size} queries")
               call.response.header("Access-Control-Allow-Origin", "*")

               println(workload)
               call.respondText("OK", ContentType.Application.Any)
           }

           get("api/getResults"){

           }
           get("api/startBenchmark"){

               val latch = CountDownLatch(threads - 1)

               if (workload == null)log.info("Attempt to start Benchmark without prior set Workload. Abort benchmark start!")
               else {
                   log.info("Start benchmark run...")
                   status = "running"
                   executor = Executors.newFixedThreadPool(threads)
                   for (i in 1 .. numberOfThreadsPerVersion){
                        val workerA = WorkerThread("Worker${i}A", socketsA, getSutList(
                            ipIndexAndOccurrence,
                            workload?.size ?: 0
                        ), workload!!, latch)

                       // executor.execute(workerA)

                       workerA.start()

                       val workerB = WorkerThread("Worker${i}A", socketsA, getSutList(
                           ipIndexAndOccurrence,
                           workload?.size ?: 0
                       ), workload!!, latch)

                       // executor.execute(workerB)

                       workerB.start()


                   }

                   latch.countDown()

                   log.info("Benchmark started")
                   call.response.header("Access-Control-Allow-Origin", "*")
                   call.respondText("OK", ContentType.Application.Json)
               }



           }
       }
    }.start(wait=true)

/*
    // Take as program args
    val ipAddresses : Array<String> = arrayOf("35.187.119.157","35.195.248.109","34.77.86.239")
    val queryIntensity: Array<Double> = arrayOf(3.3,3.3,1.0)

    val cassandraLoadQueriesList = listOf<String>(args[0])
    val cassandraRunQueriesList = listOf<String>(args[1])



    try {

        val socketsA = mutableListOf<InetSocketAddress>()
        val socketsB = mutableListOf<InetSocketAddress>()

        var ipIndexAndOccurrence = mutableMapOf<Int, Double>()

        for(index in ipAddresses.indices){
            ipIndexAndOccurrence.put(index, queryIntensity[index])
        }

        val ipIndices = getSutList(ipIndexAndOccurrence, 1_000)


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


        val latch = CountDownLatch(1)

        var workerA1 = WorkerThread("WorkerA1", socketsA, ipIndices, cassandraLoadQueriesList, cassandraRunQueriesList, latch, )
        workerA1.start()
        var workerB1 = WorkerThread("WorkerB1", socketsB, ipIndices, cassandraLoadQueriesList, cassandraRunQueriesList, latch, )
        workerB1.start()

        // Resume the execution of the threads simultaneously
        // Also possible to use CyclicBarrier Class or Phaser Class
        latch.countDown()

    } catch (e: java.lang.Exception) {


        println("Not working")
        println(e.printStackTrace())

    }

 */
}





