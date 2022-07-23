import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.SyncCqlSession
import java.net.InetSocketAddress

fun main() {
    var nodeIps = listOf<String>("34.76.234.160", "34.140.103.239", "34.78.62.250")
    var datacenters = listOf<String>("europe-west1", "europe-west1", "europe-west1")
    var run = false


    val workload="src\\main\\resources\\workloadA\\run_operations.dat"

    val ca = CassandraQueries()

    var pathToTransformedOps = ""


    val genericQueriesList = returnQueryListFromFile(workload, 1250000)

    if (!run){
        pathToTransformedOps = "src\\main\\resources\\transformed_load.dat"
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
        ca::transformLoadOperations)
    }
    if (run){
        pathToTransformedOps = "src\\main\\resources\\transformed_run.dat"
        writeTransformedOperationsToFile(pathToTransformedOps, genericQueriesList,
            ca::transformRunOperations)
    }



    var cassandraQueriesList = returnQueryListFromFile(pathToTransformedOps, 1250000)

    var queriesWithIds = assignIdsToQueries(cassandraQueriesList)

    cassandraQueriesList.toMutableList().clear()

    var socket = InetSocketAddress(nodeIps[0], 9045)
    var session: SyncCqlSession

    val builder = CqlSession.builder().withLocalDatacenter(datacenters[0])
    builder.addContactPoint(socket)
    session  = builder.build()

    var results = mutableListOf<Row?>()




    val startTime = System.currentTimeMillis()
    for((index,query) in queriesWithIds.withIndex()){
        var result = session.execute(query.second).one()
        results.add(result)
        // if(index == 300)break

    }

    println("Execution time ${System.currentTimeMillis()-startTime} ms.")




    for((index, row) in results.withIndex()){
        var lineone : String = ""
        var linetwo : String = ""
        try {
            if (row != null) {
                lineone = row.getString(0).toString()
                linetwo = row.getString(5).toString()
            }

        }catch (e: java.lang.Exception){

        }
        println("Line $index: First row --> $lineone, second row --> $linetwo")
    }

}


