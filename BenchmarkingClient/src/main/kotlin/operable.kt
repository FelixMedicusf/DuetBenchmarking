interface operable {

    fun transformLoadingPhaseOperation(operation: String, dataBaseName: String="ycsb"): String


    fun transformRunPhaseOperation(operation: String, dataBaseName: String ="ycsb"): String


}