import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.Row


fun main(args: Array<String>) {


    try{
        var session1 = CassandraConnector()
        session1.connect("35.189.219.44", 9045, "datacenter1")



    }catch(e: java.lang.Exception){
        print("Connection Class failed")
    }


    try {

        /* Tie Keyspace "ycsb" to the Session "sessionForKeyspace". This keyspace will then be used as the default when
        using session.execute */
        val sessionForKeyspace = CqlSession.builder()
            .withKeyspace(CqlIdentifier.fromCql("ycsb"))
            .build()

        var session: CqlSession = CqlSession.builder().build()

        /* You can run queries with the session's execute method.
        Executing a query produces a ResultSet, which is an iterable of Row. The basic way to process all rows is to use
        Java's for-each loop. When you know that there is only one row (or are only interested in the first one), the
        driver provides a convenient method: one(). Row provides getters to extract column values; they can be positional
        or named*/
        var rs : ResultSet = sessionForKeyspace.execute("select release_version from system.local")
        var row : Row? = rs.one()
        println(row?.getString("release_version"))
        session.close()


    } catch (e: java.lang.Exception) {
        print("Connection Script failed")
    }

}