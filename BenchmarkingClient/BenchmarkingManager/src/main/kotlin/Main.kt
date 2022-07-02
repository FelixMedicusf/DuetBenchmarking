import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

suspend fun main (args: Array<String>){

    val workloadAPath = "src\\main\\resources\\workloadA"
    val workloadCPath = "src\\main\\resources\\workloadC"
    val workloadEPath = "src\\main\\resources\\workloadE"

    // Passes as Arguments to jar file in the end ...
    val queryNumber = 5
    val workerIps = args

    val listOfIds = mutableListOf<String>()

    val ca = CassandraQueries()

    val genericLoadQueriesList = returnQueryListFromFile("$workloadEPath\\load_operations.dat")

    writeTransformedOperationsToFile("$workloadEPath\\load_operations_cassandra.dat", genericLoadQueriesList,
        ca::transformLoadOperations)

    val genericRunQueriesList = returnQueryListFromFile("$workloadEPath\\run_operations.dat")

    writeTransformedOperationsToFile("$workloadEPath\\run_operations_cassandra.dat", genericRunQueriesList,
        ca::transformRunOperations)

    var cassandraLoadQueriesList = returnQueryListFromFile("$workloadEPath\\load_operations_cassandra.dat")

    var cassandraRunQueriesList = returnQueryListFromFile("$workloadEPath\\run_operations_cassandra.dat")




    var queriesWithIds = assignIdsToQueries(cassandraLoadQueriesList)

    // Replace numberOfWorker with args.size (number of ipAddresses equals number of nodes)
    var queriesPerWorkerWithIds = divideQueryList(3, queriesWithIds)


    var file = "src\\main\\resources\\newFile.txt"



    val client = HttpClient(CIO)
    val url = "http://localhost:8080"


    // Send every #{node}th transformed query to the worker
    client.post("$url/api/setWorkload") {
        val content = Json.encodeToString(queriesPerWorkerWithIds[0])
        setBody(content)
    }

    /*
    for ((index, ip) in workerIps.withIndex()){
        client.post(ip){
             val queriesPerWorkerAsJson = Json.encodeToString(queriesPerWorkerWithIds[index])
             setBody(queriesPerWorkerAsJson)
        }
    }
    */

    var ipsAndFrequencies = listOf<Pair<String, Double>>(Pair("2232",0.4), Pair("4343", 0.4), Pair("23423", 0.4))

    client.post("$url/api/setNodesAndFrequencies"){
        val content = Json.encodeToString(ipsAndFrequencies)
        setBody(content)
    }

    // Start Benchmark
    client.get("$url/api/startBenchmark")


}
