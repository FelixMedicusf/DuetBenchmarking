import java.util.*


interface Operable2 {

    fun read(database: String, table: String, fields:Set<String>, result: Map<String, ByteIterator>): String

    fun scan(
        database: String,
        table: String, startKey: String, recordCount: Int, fields: Set<String>,
        result: Vector<HashMap<String, ByteIterator>>
    ): String

    fun update(database: String, table: String, key: String, values: Map<String, ByteIterator>): String

    fun insert(database: String, table: String, key: String, values: Map<String, ByteIterator>): String

    fun delete(table: String, key: String): String


}
