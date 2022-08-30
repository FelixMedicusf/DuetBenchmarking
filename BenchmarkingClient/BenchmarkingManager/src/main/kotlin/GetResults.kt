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
import java.nio.file.Paths
var run = false
var workerIps = listOf("34.76.252.7","34.105.161.168","34.141.79.78")
var regions = listOf("europe-west1", "europe-west2", "europe-west3")
suspend fun main() {

    val client = HttpClient(CIO){
        // disable request timeout cause benchmark takes time
        engine {
            requestTimeout = 0
            endpoint.connectTimeout = 100000
            endpoint.connectAttempts = 5

        }
    }

    val totalMeasurements = mutableListOf<Measurement>()
    runBlocking {
        val receivedFrom = arrayListOf<Int>()
        while(receivedFrom.size < workerIps.size) {

            for ((index, ip) in workerIps.withIndex()) {
                if(index !in receivedFrom) {
                    val url = "http://$ip:8080"
                    println("Send getResults-Request to $url")
                    val response1 = client.get("$url/api/getResultsFirst")
                    val response2 = client.get("$url/api/getResultsSecond")
                    if (response1.status == HttpStatusCode.OK && response2.status == HttpStatusCode.OK) {
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response1.bodyAsText())
                        totalMeasurements += Json.decodeFromString<List<Measurement>>(response2.bodyAsText())
                        response1.discardRemaining()
                        response2.discardRemaining()

                        receivedFrom += index
                        println("Received results from worker $index")

                    }
                }
            }
        }
    }


    val cwd = System.getProperty("user.dir")
    var path = ""


    if(!run) {
        try {
            path = Paths.get(cwd, "load_measurements.csv").toString()
            writeMeasurementsToCsvFile(path, totalMeasurements, regions)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    if(run) {
        try {
            path = Paths.get(cwd, "run_measurements.csv").toString()
            writeMeasurementsToCsvFile(path, totalMeasurements, regions)
            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }
    }
    println("Wrote all measurements to file $path")
}