import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.CqlSessionBuilder
import com.datastax.oss.driver.api.core.cql.ResultSet
import com.datastax.oss.driver.api.core.cql.Row
import java.net.InetSocketAddress


    public class CassandraConnector {


           lateinit var session: CqlSession;


        fun connect(node: String, port: Int, dataCenter:String){
            var builder : CqlSessionBuilder = CqlSession.builder()

            builder.addContactPoint(InetSocketAddress(node, port))

           //  builder.withLocalDatacenter(dataCenter)

           session = builder.build()

            var rs : ResultSet = session.execute("select release_version from system.local")

            var k : ResultSet = session.execute("SELECT * FROM ycsb.usertable")
            var row : Row? = rs.one()


            for(row in k){
                println(row)
            }
            println(row)

        }


        fun getSession1(): CqlSession = this.session

        fun close() {

            return session.close()
        }



    }
