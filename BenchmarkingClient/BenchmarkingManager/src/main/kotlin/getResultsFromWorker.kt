import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import jdk.jfr.ContentType
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

var results = mutableListOf<Triple<String, Long, Long,>>()

suspend fun main(){

val client = HttpClient(CIO)
val url = "http://35.205.205.137:8080"


    val results: String= client.get("http://34.76.191.126:8080/api/getFirstResults").bodyAsText()

    val list = Json.decodeFromString<List<Measurement>>(results)

    println(list.size)

    println(results)


}