@kotlinx.serialization.Serializable
data class Measurement(val w: String, var q:String?, val id:String, val s:Long, val r:Long, var n:String){
    override fun toString(): String {
        return "Measurement {worker: '${w}', query: '$q', id: '$id', sent: $s, received: $r}"
    }
}
