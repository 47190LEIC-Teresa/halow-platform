package backend.db

import java.sql.Connection
import java.sql.DriverManager

object Database {

    private const val URL = "jdbc:postgresql://localhost:5432/postgres"
    private const val USER = "tekas"
    private const val PASSWORD = ""

    fun getConnection(): Connection {
        return DriverManager.getConnection(URL, USER, PASSWORD)
    }
}