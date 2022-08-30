import java.nio.file.Paths
import kotlin.math.ceil

fun getSutList(ipsAndAbsoluteOccurence:Map<Int, Double>, numberOfOperations: Int): List<Int>{

    var ipAddressIndices = mutableListOf<Int>()

    val sum = ipsAndAbsoluteOccurence.values.sum()

    var ipIndicesAndRelativeOccurrence = mutableMapOf<Int, Double>()


    for ((key, value) in ipsAndAbsoluteOccurence) ipIndicesAndRelativeOccurrence[key] = value/sum


    var firstList = mutableListOf<Int>()

    for((key, value) in ipIndicesAndRelativeOccurrence) {
        var number = (value * 100).toInt()
        var i = 0
        while(i < number) {
            firstList.add(key)

            i++
        }
    }
    firstList.shuffle()

    var x = 0
    while(x < numberOfOperations){
        var randomNumber = (0 until firstList.size).random()
        var randomIpIndex = firstList[randomNumber]
        ipAddressIndices.add(randomIpIndex)

        x++
    }
    return ipAddressIndices
}

fun main(){

    // write Results to file
    val cwd = System.getProperty("user.dir")
    var path = ""

    if(false) {
        try {
            path = Paths.get(cwd, "load_measurements.csv").toString()

            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }

    }
    if(true) {
        try {
            path = Paths.get(cwd, "run_measurements.csv").toString()

            // writeResultsToFile("~/Documents/DuetBenchmarking/measurements.dat", totalMeasurements)
        } catch (e: java.lang.Exception) {

        }

    }
    println("Wrote all measurements to file $path")
}