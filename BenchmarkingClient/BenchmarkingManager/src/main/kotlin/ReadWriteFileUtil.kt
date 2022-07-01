import java.io.File

fun writeOperationsToFile (fileName: String, operationsList: List<String>, transformLine:(String, String) -> String ){
    var file = File(fileName)

    file.printWriter().use { out ->
        for (line in operationsList){
            out.println(transformLine(line, "ycsb"))
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