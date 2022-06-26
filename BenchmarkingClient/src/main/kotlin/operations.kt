import java.io.File
import java.util.stream.Collectors



class cassandraDataBaseQueries: operable{


    override fun transformLoadingPhaseOperation(operation : String, dataBaseName: String): String {

        var cassandraOperation = operation
            .replace("INSERT", "INSERT INTO")
            .replace("usertable", "${dataBaseName}.usertable")

        cassandraOperation = cassandraOperation
            .substring(0, cassandraOperation.indexOf("${dataBaseName}.usertable") + dataBaseName.length + 10)

        var insertData = operation.substring(operation.indexOf("usertable") + "usertable".length + 1).replace("\'", "\"")

        val columnList : List<String> = listOf("y_id", "field0", "field1", "field2", "field3", "field4", "field5", "field6", "field7",
                                                "field8", "field9")

        var columnValues = mutableListOf<String>()

        var values = insertData.split("field").toMutableList()



        values = values.sorted().toMutableList()

        var last = values[values.size - 1]
        values.add(0, last)

        values.removeAt(values.size - 1)

        var n = 0

        while(n < values.size){

            var value = ""
            if(n == 0) {

                if(values[0].contains(" ")){
                    value = values[0].substring(0, values[0].indexOf(" "));
                    columnValues.add(value)
                }

            } else {

                value = values[n].substring(2, 102)
                columnValues.add(value)
            }
            n++
        }

        return "${cassandraOperation} (${columnList.joinToString { x -> x}}) VALUES " +
                "(${columnValues.joinToString {  x -> "\'$x\'" }})"
    }




    override fun transformRunPhaseOperation(operation: String, dataBaseName: String): String {

        // TODO("ESCAPE SPECIAL CHARACTERS")
        var removedBrackets = operation.replace("[", "").replace("]","")

        var components: MutableList<String> = removedBrackets.split(" ").toMutableList()

        val typeOfOperation = components[0]

        var cassandraOperation = ""

        when(typeOfOperation){
            "READ"-> {
                cassandraOperation += "SELECT "

                if (components.contains("<all")){
                    cassandraOperation+="* "
                }

                cassandraOperation+="FROM "

                cassandraOperation+="$dataBaseName.${components[1]} "

                cassandraOperation+="WHERE y_id=\'${components[2]}\';"
            }
            "UPDATE"-> {
                val data = components.subList(3, components.size).joinToString(separator = "")

                val insertData = data.replace("$", "d").replace("\"", "q")
                                .replace("\'","f").substring(7)

                val field = data.substring(0, 6)

                cassandraOperation+="UPDATE "
                cassandraOperation+="$dataBaseName.${components[1]} "
                cassandraOperation+="SET $field=\'$insertData\' "
                cassandraOperation+="WHERE y_id=\'${components[2]}\';"
            }
        }
        return cassandraOperation
    }



}


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

fun returnListFromRunData(fileName: String) : List<String> {
    val lines: MutableList<String> = mutableListOf()
    File(fileName).readLines().forEach { line ->
        if (line.startsWith("READ") || line.startsWith("UPDATE")) {
            lines.add(line)
        }
    }
    return lines;
}

fun main(){
    var ca = cassandraDataBaseQueries()

    /*

    var loadingOperationsList = returnListFromInsertData("src\\main\\resources\\loading_operations.dat")

    writeOperationsToFile("src\\main\\resources\\loading_operations_cassandra.dat", loadingOperationsList,  ca::transformLoadingPhaseOperation)


    for (line in returnListFromInsertData("src\\main\\resources\\loading_operations_cassandra.dat")){
        println(line)
    }

     */
    var UPDATE = "UPDATE usertable user1709515506375118236 [ field1=1% Za;:h?n'Yq>8r;M5,':8Z=%H3?Sy;'23!>9&4 789Y)>^c;*&:$:1S-- 4 *8-j(0*L% !2\$Y9<?h;.~/8n)5j3]9:)`; ]"

    var READ = "READ usertable user1935691084326388114 [ <all fields>]"

    println(ca.transformRunPhaseOperation(READ, "ycsb"))
    println("""
        
       
    """.trimIndent())
    println(ca.transformRunPhaseOperation(UPDATE, "ycsb"))


}
