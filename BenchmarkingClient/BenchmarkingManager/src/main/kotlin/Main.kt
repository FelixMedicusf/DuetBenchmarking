import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch


fun main(args: Array<String>) {

    // Take as program args
    val ipAddresses : Array<String> = arrayOf("35.187.119.157","35.195.248.109","34.77.86.239")
    val queryIntensity: Array<Double> = arrayOf(3.3,3.3,1.0)


    val workloadAPath = "src\\main\\resources\\workloadA"
    val workloadCPath = "src\\main\\resources\\workloadC"
    val workloadEPath = "src\\main\\resources\\workloadE"



    try {

        val socketsA = mutableListOf<InetSocketAddress>()
        val socketsB = mutableListOf<InetSocketAddress>()

        /* Read the from YCSB generated queries and transform them to Cassandra-specific queries and place them in the list
         "queries" */

        val ca = CassandraQueries()

        val genericLoadQueriesList = returnQueryListFromFile("$workloadEPath\\load_operations.dat")

        writeOperationsToFile("$workloadEPath\\load_operations_cassandra.dat", genericLoadQueriesList,
            ca::transformLoadOperations)

        val genericRunQueriesList = returnQueryListFromFile("$workloadEPath\\run_operations.dat")

        writeOperationsToFile("$workloadEPath\\run_operations_cassandra.dat", genericRunQueriesList,
            ca::transformRunOperations)



        var cassandraLoadQueriesList = returnQueryListFromFile("$workloadEPath\\load_operations_cassandra.dat")

        var cassandraRunQueriesList = returnQueryListFromFile("$workloadEPath\\run_operations_cassandra.dat")


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



