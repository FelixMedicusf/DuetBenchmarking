import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch


fun main(args: Array<String>) {

    // Take as program args
    val ipAddresses : Array<String> = arrayOf("35.205.128.75","34.79.99.119","34.140.43.140")
    val queryIntensity: Array<Double> = arrayOf(3.3,3.3,1.0)

    try {

        val socketsA = mutableListOf<InetSocketAddress>()
        val socketsB = mutableListOf<InetSocketAddress>()

        /* Read the from YCSB generated queries and transform them to Cassandra-specific queries and place them in the list
         "queries" */

        var ca = cassandraDataBaseQueries()

        var loadingOperationsList = returnListFromInsertData("src\\main\\resources\\loading_operations.dat")
        writeOperationsToFile("src\\main\\resources\\loading_operations_cassandra.dat", loadingOperationsList,
            ca::transformLoadingPhaseOperation)

        var runningOperationsList = returnListFromRunData("src\\main\\resources\\run_operations.dat")
        writeOperationsToFile("src\\main\\resources\\run_operations_cassandra.dat", runningOperationsList,
            ca::transformRunPhaseOperation)



        var loadOperations = returnListFromInsertData("src\\main\\resources\\loading_operations_cassandra.dat")

        var runOperations = returnListFromInsertData("src\\main\\resources\\run_operations_cassandra.dat")


        var ipIndexAndOccurrence = mutableMapOf<Int, Double>()

        for(index in ipAddresses.indices){
            ipIndexAndOccurrence.put(index, queryIntensity[index])
        }

        val ipIndices = getSutList(ipIndexAndOccurrence, 1_000)


        try {
            for (address in ipAddresses) {
                var socketA = InetSocketAddress(address, 9045)
                var socketB = InetSocketAddress(address, 9050)
                socketsA.add(socketA)
                socketsB.add(socketB)
            }

        }catch(e: java.lang.Exception){
            e.printStackTrace()
        }


        val latch = CountDownLatch(1)

        var workerA1 = WorkerThread("WorkerA1", socketsA, ipIndices, loadOperations, runOperations, latch, )
        workerA1.start()
        var workerB1 = WorkerThread("WorkerB1", socketsB, ipIndices, loadOperations, runOperations, latch, )
        workerB1.start()

        // Resume the execution of the threads simultaneously
        // Also possible to use CyclicBarrier Class or Phaser Class
        latch.countDown()


        /*

        var sessionsA = mutableListOf<CqlSession>()
        var sessionsB = mutableListOf<CqlSession>()

        for (socket in socketsA){
            var builder = CqlSession.builder().withLocalDatacenter("europe-west1")

            builder.addContactPoint(socket)

            sessionsA.add(builder.build())
        }
        for (socket in socketsA){
            var builder = CqlSession.builder().withLocalDatacenter("europe-west1")

            builder.addContactPoint(socket)

            sessionsB.add(builder.build())
        }

        for((index, query) in queries.withIndex()){
            var nodeNumber = ipIndices[index]

            sessionsA[nodeNumber].execute(query)
            sessionsB[nodeNumber].execute(query)


            // mache query an
        }






        var socket2 = InetSocketAddress(ipAddresses[1], 9050)
        var socket1 = InetSocketAddress(ipAddresses[0], 9050)

        // Cluster does not exist anymore; the session is now the main component, initialized in a single step

        var builder: CqlSessionBuilder = CqlSession.builder().withLocalDatacenter("europe-west1")
        builder.addContactPoint(socket2).addContactPoint(socket1)

        var session2 = builder.build()

        session2.execute("Hello")




        println(queries[0])

        var query = "SELECT * FROM ycsb.usertable;"
        session2.execute(query)

        session2.execute(queries[0])


        var resultsFromSelectStatementQuery = session2.execute(query)


        var firstRow: Row? = resultsFromSelectStatementQuery.one()

        for(i in 0..10){
            println(firstRow?.getString(i))
        }

        // println(resultsFromFirstQuery)

        session2.close()

         */

    } catch (e: java.lang.Exception) {


        println("Not working")
        println(e.printStackTrace())

    }
}



