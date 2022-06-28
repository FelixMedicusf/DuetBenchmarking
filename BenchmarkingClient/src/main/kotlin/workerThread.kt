import com.datastax.oss.driver.api.core.CqlSession
import java.net.InetSocketAddress
import java.sql.ResultSet
import java.time.Instant
import java.util.concurrent.CountDownLatch

class WorkerThread(val WorkerName:String, private val sockets: List<InetSocketAddress>, val ipIndices: List<Int>,
                   val loadOperations: List<String>, val runOperations: List<String>,
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

        // After the Threads are started they are blocked immediately
        latch.await()
        println("$WorkerName started Data Loading at ${Instant.now()}")
        val startTimeLoading = System.currentTimeMillis()
        for((index, query) in loadOperations.withIndex()){
            var nodeNumber = ipIndices[index]
            sessions[nodeNumber].execute(query)
        }

        println("Finished Data Loading of: $WorkerName in ${System.currentTimeMillis()-startTimeLoading} milliseconds.")

        println("$WorkerName started Data Running Queries at ${Instant.now()}")
        val startTimeRunning = System.currentTimeMillis()


        for((index, query) in runOperations.withIndex()){
            var nodeNumber = ipIndices[index]
            var result = sessions[nodeNumber].execute(query)
            runResults.add(result)
        }


        for(session in sessions){
            session.close()
        }

        println("Finished Running Phase of: $WorkerName in ${System.currentTimeMillis()-startTimeRunning} milliseconds.")

        println("Overall Execution time of $WorkerName are ${System.currentTimeMillis()-startTimeLoading} milliseconds.")

    }
}