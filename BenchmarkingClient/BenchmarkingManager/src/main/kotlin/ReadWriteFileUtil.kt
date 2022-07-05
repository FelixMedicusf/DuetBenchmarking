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

fun writeResultsToFile(fileName: String, measurements: List<Triple<String, Long, Long>>): Unit {
    var file = File(fileName)
    file.printWriter().use { out ->
        for (measurement in measurements) {
            out.println(measurement)
        }
    }
}



fun main (){
    var totalMeasurements = mutableListOf<Triple<String, Long, Long>>(Triple("213", 1, 3), Triple("233", 1, 3))
    writeResultsToFile("C:\\Users\\Felix Medicus\\Dokumente\\measurements.dat", totalMeasurements)
}
