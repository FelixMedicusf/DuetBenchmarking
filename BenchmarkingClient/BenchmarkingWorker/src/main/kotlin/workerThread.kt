import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.core.DriverTimeoutException
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.core.cql.SyncCqlSession
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.time.Instant
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

var latencies: MutableList<Measurement> = Collections.synchronizedList(mutableListOf<Measurement>())

//var workloadA = Collections.synchronizedList(listOf<Pair<String, String>>())
//var workloadB = Collections.synchronizedList(listOf<Pair<String, String>>())

//var indexA: AtomicInteger = AtomicInteger(0)
//var indexB: AtomicInteger = AtomicInteger(0)


class WorkerThread(
    private val workerName:String, private val sockets: List<InetSocketAddress>, val ipIndices: List<Int>,
    val workload: List<Pair<Int, String>>, private val region: String,
    private val latch: CountDownLatch): Thread() {

    var sessions = mutableListOf<SyncCqlSession>()
    private var session: SyncCqlSession


    var results = mutableListOf<Row>()

    init {
        val builder = CqlSession.builder().withLocalDatacenter(region)
        builder.addContactPoint(sockets[0])
        session  = builder.build()

        println("$workerName connected to the Cluster!")

    }

    override fun run() {
        // After the Threads are started they are blocked immediately
        latch.await()
        println("$workerName started Workload Querying at ${Instant.now()}")
        val startTime = System.currentTimeMillis()


            for ((index, query) in workload.withIndex()) {
                // val nodeNumber = ipIndices[index]

                val startTimeSingleQuery = System.nanoTime()

                //var result = sessions[nodeNumber].execute(query.second)
                var endTimeSingleQuery = 0L

                endTimeSingleQuery = try {
                    var result = session.execute(query.second).one()
                    System.nanoTime()

                }catch(e: DriverTimeoutException){
                    999999999999999999
                }

                latencies.add(Measurement(workerName, query.second.split(" ")[0], query.first, startTimeSingleQuery, endTimeSingleQuery, "unknown"))

                if (index == ceil((workload.size).toDouble()/2.0).toInt())println("Half of the queries processed by $workerName")
            }

/*
        if (numberOfThreadsPerVersion > 1) {
            if (workerName.contains("a")) {

                while (indexA.get() < workloadA.size) {
                    val index = indexA.getAndIncrement()
                    var nodeNumber = ipIndices[index]
                    val startTimeSingleQuery = System.currentTimeMillis()
                    sessions[nodeNumber].execute(workloadA[index].second)
                    val endTimeSingleQuery = System.currentTimeMillis()

                    latencies.add(Measurement(workerName, workloadA[index].second.split(" ")[0], workloadA[index].first, startTimeSingleQuery, endTimeSingleQuery, nodeNumber.toString()))


                }
            }
        }
        if (workerName.contains("b")) {
            while (indexB.get() < workloadB.size) {
                var index = indexB.getAndIncrement()
                var nodeNumber = ipIndices[index]
                val startTimeSingleQuery = System.currentTimeMillis()
                sessions[nodeNumber].execute(workloadB[index].second)
                val endTimeSingleQuery = System.currentTimeMillis()

                latencies.add(Measurement(workerName, workloadA[index].second.split(" ")[0], workloadA[index].first, startTimeSingleQuery, endTimeSingleQuery, nodeNumber.toString()))


            }
        }
*/

        println("Finished Workload Querying of: $workerName in ${System.currentTimeMillis() - startTime} milliseconds.")

        // println(results.size)

        /*
        for (session in sessions) {
            session.close()
        }
        */
         session.close()
    }
}



