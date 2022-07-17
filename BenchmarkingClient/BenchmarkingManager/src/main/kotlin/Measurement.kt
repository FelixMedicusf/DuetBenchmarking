@kotlinx.serialization.Serializable
data class Measurement(val w: String, var q:String, val id:Int, val s:Long, val r:Long, var n:String){
    override fun toString(): String {
        return "$w,$q,$id,$s,$r,$n"
    }
}
