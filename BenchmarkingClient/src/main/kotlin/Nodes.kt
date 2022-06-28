fun getSutList(ipsAndAbsoluteOccurence:Map<Int, Double>,  numberOfOperations: Int): List<Int>{

    var ipAddressIndices = mutableListOf<Int>()

    val sum = ipsAndAbsoluteOccurence.values.sum()

    var ipIndicesAndRelativeOccurence = mutableMapOf<Int, Double>()


    for ((key, value) in ipsAndAbsoluteOccurence) ipIndicesAndRelativeOccurence.put(key, value/sum)


    var firstList = mutableListOf<Int>()

    for((key, value) in ipIndicesAndRelativeOccurence) {
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
        var randomNumber = (0..firstList.size - 1).random()
        var randomIpIndex = firstList[randomNumber]
        ipAddressIndices.add(randomIpIndex)

        x++
    }

    return ipAddressIndices

}


fun main(){
    val ipIndexAndOccurence = mapOf<Int, Double>(0 to 4.5, 1 to 4.5, 2 to 1.0)
    println(getSutList(ipIndexAndOccurence, 100_000))

}