import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import java.util.*
import kotlin.math.ceil


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

    var latencies = listOf<Int>(1,2,3,4,5,6,7,78,8,9,0,0,3,5,6)
    println(latencies.chunked(ceil((latencies.size).toDouble()/4).toInt())[2])
}
