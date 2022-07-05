import java.util.UUID
import java.util.Vector

fun divideQueryList(numberOfWorkers: Int, queryList: List<Pair<String, String>>): List<List<Pair<String, String>>>{

    // val numberOfQueries = queryList.size

    var queryListsForWorkersWithIds = mutableListOf<MutableList<Pair<String, String>>>()

    var x = 0

    for((index, query) in queryList.withIndex()){

        if (index < numberOfWorkers)queryListsForWorkersWithIds.add(mutableListOf(query))

        if(index >= numberOfWorkers) {
            for (i in 0 until numberOfWorkers) {
                if (i == x) queryListsForWorkersWithIds[i].add(query)
            }
             x = if(x<numberOfWorkers-1)x+1 else 0
        }
    }
    return queryListsForWorkersWithIds
}

fun assignIdsToQueries(queryList: List<String>):List<Pair<String, String>> {

    var idList = mutableListOf<String>()

    for (i in queryList.indices) {
        // Might produce same Ids.
        var id = UUID.randomUUID().toString();
        idList.add(id)
    }

    var queriesWithIds = mutableListOf<Pair<String, String>>()

    for(i in queryList.indices){
        queriesWithIds.add(Pair(idList[i], queryList[i]))
    }

    return queriesWithIds

}




fun main(){
    var list = listOf<String>("a", "b", "c", "d", "e", "f", "g", "h")

    var x = assignIdsToQueries(queryList = list)

    var y = divideQueryList(3, x)

    print(y)


}

