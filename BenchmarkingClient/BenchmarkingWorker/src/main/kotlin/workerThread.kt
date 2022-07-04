import com.datastax.oss.driver.api.core.CqlSession
import java.net.InetSocketAddress
import java.sql.ResultSet
import java.time.Instant
import java.util.Collections
import java.util.concurrent.CountDownLatch

var latencies = Collections.synchronizedList(mutableListOf<Triple<String, Long, Long>>())


class WorkerThread(val WorkerName:String, private val sockets: List<InetSocketAddress>, val ipIndices: List<Int>,
                   val workload: List<Pair<String, String>>, val datacenters: List<String>,
                   private val latch: CountDownLatch): Thread() {

    var sessions = mutableListOf<CqlSession>()

    init {
        for((index, socket) in sockets.withIndex()){

            var builder = CqlSession.builder().withLocalDatacenter(datacenters[index])
            builder.addContactPoint(socket)
            sessions.add(builder.build())

            println("$WorkerName initialized session to $socket")
        }
    }

    override fun run() {

        // After the Threads are started they are blocked immediately
        latch.await()

        println("$WorkerName started Workload Querying at ${Instant.now()}")
        val startTime = System.currentTimeMillis()
        for ((index, query) in workload.withIndex()) {
            var nodeNumber = ipIndices[index]
            val startTimeSingleQuery = System.currentTimeMillis()
            sessions[nodeNumber].execute(query.second)
            val endTimeSingleQuery = System.currentTimeMillis()

            latencies.add(Triple("${query.first}/${WorkerName}", startTimeSingleQuery, endTimeSingleQuery))

        }

        println("Finished Workload Querying of: $WorkerName in ${System.currentTimeMillis() - startTime} milliseconds.")

        for (session in sessions) {
            session.close()
        }
    }
}