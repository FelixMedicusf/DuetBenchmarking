import com.datastax.oss.driver.api.core.CqlSession
import java.net.InetSocketAddress
import java.sql.ResultSet
import java.time.Instant
import java.util.concurrent.CountDownLatch


class WorkerThread(val WorkerName:String, private val sockets: List<InetSocketAddress>, val ipIndices: List<Int>,
                   val workload: List<Pair<String, String>>, val datacenters: List<String>,
                   private val latch: CountDownLatch): Thread() {

    var sessions = mutableListOf<CqlSession>()

    var runResults = mutableListOf<com.datastax.oss.driver.api.core.cql.ResultSet>()

    init {
        for(socket in sockets){

            var builder = CqlSession.builder().withLocalDatacenter("europe-west1")
            builder.addContactPoint(socket)
            sessions.add(builder.build())

            println("$WorkerName initialized session to $socket")
        }
    }

    override fun run() {

        // Implement Measurement for query latency


        // After the Threads are started they are blocked immediately

        latch.await()
        println("$WorkerName started Workload Querying at ${Instant.now()}")
        val startTimeLoading = System.currentTimeMillis()
        for((index, query) in workload.withIndex()){
            var nodeNumber = ipIndices[index]
            sessions[nodeNumber].execute(query.second)
        }

        println("Finished Workload Querying of: $WorkerName in ${System.currentTimeMillis()-startTimeLoading} milliseconds.")

        for(session in sessions){
            session.close()
        }

    }

}
