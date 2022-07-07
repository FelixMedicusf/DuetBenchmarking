import com.datastax.oss.driver.api.core.CqlSession
import java.net.InetSocketAddress



val ipAddresses : Array<String> = arrayOf("34.77.134.234","34.79.157.65","34.79.111.153")

fun main() {
    val socketsA = mutableListOf<InetSocketAddress>()
    val socketsB = mutableListOf<InetSocketAddress>()


    for (address in ipAddresses) {
        var socketA = InetSocketAddress(address, 9045)
        var socketB = InetSocketAddress(address, 9050)
        socketsA.add(socketA)
        socketsB.add(socketB)
    }

    deleteAll(socketsA)
    deleteAll(socketsB)

}


fun deleteAll(sockets: List<InetSocketAddress>){
    var builder = CqlSession.builder().withLocalDatacenter("europe-west1")
    builder.addContactPoint(sockets[0])
    var session = (builder.build())

    session.execute("TRUNCATE ycsb.usertable;")
    session.close()
}

