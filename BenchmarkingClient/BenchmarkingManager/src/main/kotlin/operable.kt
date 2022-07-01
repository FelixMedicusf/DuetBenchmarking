interface operable {

    fun transformLoadOperations(operation: String, dataBaseName: String="ycsb"): String


    fun transformRunOperations(operation: String, dataBaseName: String ="ycsb"): String


}