@kotlinx.serialization.Serializable
data class Measurement(val workerInstance: String, val query:String, val id:String?, val sentTime:Long, val receiveTime:Long)
