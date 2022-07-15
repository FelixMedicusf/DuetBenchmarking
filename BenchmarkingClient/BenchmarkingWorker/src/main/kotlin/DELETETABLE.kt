import com.datastax.oss.driver.api.core.CqlSession
import java.net.InetSocketAddress



val ipAddresses : Array<String> = arrayOf("35.205.125.232","104.199.97.243","34.140.118.190")

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

