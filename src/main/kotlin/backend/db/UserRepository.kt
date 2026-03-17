package backend.db

data class AppUser(
    val username: String,
    val email: String?,
    val firstName: String?,
    val lastName: String?
)

class UserRepository {

    fun createUser(user: AppUser) {
        val sql = """
            INSERT INTO app_user (username, email, first_name, last_name)
            VALUES (?, ?, ?, ?)
        """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, user.username)
                stmt.setString(2, user.email)
                stmt.setString(3, user.firstName)
                stmt.setString(4, user.lastName)
                stmt.executeUpdate()
            }
        }
    }
}