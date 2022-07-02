import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

var results = mutableListOf<Triple<String, Long, Long,>>()

suspend fun main(){

val client = HttpClient(CIO)
val url = "http://localhost:8080"

client.get("api/getResults"){
    val responseContent = body
    results = Json.decodeFromJsonElement(responseContent as JsonElement)

}
}