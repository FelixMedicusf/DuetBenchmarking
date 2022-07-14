import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.core.cql.SyncCqlSession
import kotlinx.coroutines.future.await
import java.net.InetSocketAddress
import java.time.Instant
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

var latencies = Collections.synchronizedList(mutableListOf<Measurement>())

//var workloadA = Collections.synchronizedList(listOf<Pair<String, String>>())
//var workloadB = Collections.synchronizedList(listOf<Pair<String, String>>())

//var indexA: AtomicInteger = AtomicInteger(0)
//var indexB: AtomicInteger = AtomicInteger(0)


class WorkerThread(
    private val workerName:String, private val sockets: List<InetSocketAddress>, val ipIndices: List<Int>,
    val workload: List<Pair<String, String>>, private val datacenters: List<String>,
    private val latch: CountDownLatch): Thread() {

    var sessions = mutableListOf<SyncCqlSession>()

    init {
        for ((index, socket) in sockets.withIndex()) {

            var builder = CqlSession.builder().withLocalDatacenter(datacenters[index])

            builder.addContactPoint(socket)
            val session : SyncCqlSession = builder.build()
            sessions.add(session)

            println("$workerName initialized session to $socket")
        }
    }

    override fun run() {

        // After the Threads are started they are blocked immediately
        latch.await()
        println("$workerName started Workload Querying at ${Instant.now()}")
        val startTime = System.currentTimeMillis()


            for ((index, query) in workload.withIndex()) {
                var nodeNumber = ipIndices[index]
                val startTimeSingleQuery = System.currentTimeMillis()

                var result = sessions[nodeNumber].execute(query.second)
/*
                while(!result.wasApplied()){

                }

 */
                val endTimeSingleQuery = System.currentTimeMillis()

                latencies.add(Measurement(workerName, query.second.split(" ")[0], query.first, startTimeSingleQuery, endTimeSingleQuery, nodeNumber.toString()))

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

        for (session in sessions) {
            session.close()
        }
    }
}



