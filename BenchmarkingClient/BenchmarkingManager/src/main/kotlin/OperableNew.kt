import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.*


interface Operable2 {

    fun read(database: String, table: String, fields:Set<String>, result: Map<String, ByteIterator>): String

    fun scan(
        database: String,
        table: String, startKey: String, recordCount: Int, fields: Set<String>,
        result: Vector<HashMap<String, ByteIterator>>
    ): String

    fun update(database: String, table: String, key: String, values: Map<String, ByteIterator>): String

    fun insert(database: String, table: String, key: String, values: Map<String, ByteIterator>): String

    fun delete(table: String, key: String): String


}

suspend fun main (){
    var client = HttpClient(CIO)
    var ips = listOf<String>("35.233.17.61", "34.79.124.119", "34.76.191.126")

    for((x, ip) in ips.withIndex()) {
        val url = "http://$ip:8080/api/getStatus"
        println("Worker $x: ${client.get(url).bodyAsText()}")
    }
}
