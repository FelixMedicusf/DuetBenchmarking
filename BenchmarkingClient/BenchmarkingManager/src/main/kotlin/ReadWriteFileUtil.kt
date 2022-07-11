import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

fun writeTransformedOperationsToFile (fileName: String, operationsList: List<String>, transformLine:(String, String) -> String ){
    var file = File(fileName)

    file.printWriter().use { out ->
        for (line in operationsList){
            out.println(transformLine(line, "ycsb"))
        }
    }

}

fun writeOperationsToFile (fileName: String , listToWrite: List<Pair<String, String>>){
    var file = File(fileName)

    file.printWriter().use{out ->
        for(line in listToWrite){
            out.println(line)
        }
    }
}

fun returnListFromInsertData (fileName : String) : List<String> {
    val lines : MutableList <String> = mutableListOf()
    File(fileName).readLines().forEach { line ->
        if (line.startsWith("INSERT")) {
            lines.add(line)
        }
    }
    return lines;
}

fun returnQueryListFromFile(fileName: String) : List<String> {
    val lines: MutableList<String> = mutableListOf()
    File(fileName).readLines().forEach { line ->
        if (line.startsWith("READ") || line.startsWith("UPDATE") || line.startsWith("INSERT")
            || line.startsWith("SCAN") || line.startsWith("SELECT")) {
            lines.add(line)
        }
    }
    return lines;
}

fun writeMeasurementsToFile(fileName: String, measurements: List<Measurement>, queriesWithIds: List<Pair<String, String>>, regions: List<String>): Unit {
    var file = File(fileName)
    file.printWriter().use { out ->
        for (measurement in measurements) {
            for(query in queriesWithIds){
                if(query.first==measurement.id){
                    var typeOfQuery = query.second.split(" ")[0]
                    var region = regions[(measurement.n).toInt()]
                    measurement.q = typeOfQuery
                    measurement.n = region
                    out.println(measurement)
                    break
                }
            }
        }
    }
}

