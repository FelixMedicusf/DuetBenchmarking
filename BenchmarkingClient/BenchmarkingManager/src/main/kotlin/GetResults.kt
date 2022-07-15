import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

var workerIps = listOf<String>("34.76.149.30", "104.199.104.38","34.78.247.178")
var regions = listOf<String>("europe-west1", "europe-west1", "europe-west1")
suspend fun main() {

    val client = HttpClient(CIO)

    val totalMeasurements = mutableListOf<Measurement>()
    runBlocking {
        val receivedFrom = arrayListOf<Int>()
        while(receivedFrom.size < workerIps.size) {
            // Wait for 3 minutes and then ask again for measurements from all workers. When all workers sent their
            // latency measurements leave the while loop
            delay(180000)
            for ((index, ip) in workerIps.withIndex()) {
                if(index !in receivedFrom) {
                    val url = "http://$ip:8080"
                    println("Send getResults-Request to $url")
                    val response1 = client.get("$url/api/getFirstResults")
                    val response2 = client.get("$url/api/getSecondResults")
                    val response3 = client.get("$url/api/getThirdResults")
                    val response4 = client.get("$url/api/getForthResults")
                    if (response1.status == HttpStatusCode.OK && response2.status == HttpStatusCode.OK &&
                        response3.status == HttpStatusCode.OK && response4.status == HttpStatusCode.OK) {
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response1.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response2.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response3.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response4.bodyAsText())
                        receivedFrom += index
                        println("Received results from worker $index")

                    }
                }
            }
        }
    }

    println("Received all measurements from all workers!")
    // write Results to file
    try {
        writeMeasurementsToCsvFile("C:\\Users\\Felix Medicus\\Dokumente\\load_measurements_1_000.csv",
            totalMeasurements, regions)
        // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
    } catch (e: java.lang.Exception) {
        e.printStackTrace()
    }


    println("Wrote all measurements to file ~/Documents/DuetBenchmarking/load_measurements.dat")


}