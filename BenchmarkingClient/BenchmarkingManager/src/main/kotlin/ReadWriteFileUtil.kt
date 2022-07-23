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

fun returnQueryListFromFile(fileName: String, size: Int) : List<String> {
    val lines: MutableList<String> = mutableListOf()
    File(fileName).readLines().forEach { line ->
        if (line.startsWith("READ") || line.startsWith("UPDATE") || line.startsWith("INSERT")
            || line.startsWith("SCAN") || line.startsWith("SELECT")) {
            lines.add(line)
        }
    }
    return lines.subList(0, size);
}

fun writeMeasurementsToCsvFile(fileName: String, measurements: List<Measurement>, regions: List<String>): Unit {
    var file = File(fileName)
    file.printWriter().use { out ->
        out.println("workerId,queryType,queryId,sent,received,target-region")
        for (measurement in measurements) {
                //measurement.n = regions[(measurement.n).toInt()]
                out.println(measurement)
        }
    }
}

