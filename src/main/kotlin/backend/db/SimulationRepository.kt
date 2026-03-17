package backend.db

import backend.simulator.SimulatorParams
import java.sql.Timestamp
import java.time.LocalDateTime

class SimulationRepository {

    private fun findSimulationConfig(params: SimulatorParams): Long? {
        val sql = """
        SELECT id
            FROM simulation_config
            WHERE n_groups = ?
              AND n_stations = ?
              AND width = ?
              AND height = ?
              AND verbosity = ?
              AND sim_length = ?
              AND packet_rate = ?
              AND slot_length = ?
            LIMIT 1
    """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, params.g)
                stmt.setObject(2, params.n)
                stmt.setObject(3, params.w)
                stmt.setObject(4, params.h)
                stmt.setObject(5, params.verbosity)
                stmt.setObject(6, params.simLength)
                stmt.setObject(7, params.packetRate)
                stmt.setObject(8, params.slotLength)

                val rs = stmt.executeQuery()
                return if (rs.next()) rs.getLong("id") else null
            }
        }
    }

    private fun createSimulationConfig(params: SimulatorParams): Long {
        val sql = """
            INSERT INTO simulation_config (
                n_groups, n_stations, width, height, verbosity, sim_length, packet_rate, slot_length
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setObject(1, params.g)
                stmt.setObject(2, params.n)
                stmt.setObject(3, params.w)
                stmt.setObject(4, params.h)
                stmt.setObject(5, params.verbosity)
                stmt.setObject(6, params.simLength)
                stmt.setObject(7, params.packetRate)
                stmt.setObject(8, params.slotLength)

                val rs = stmt.executeQuery()
                rs.next()
                return rs.getLong("id")
            }
        }
    }

    fun createSimulation(username: String, configId: Long, params: SimulatorParams): Long {
        val sql = """
            INSERT INTO simulation (
                username, config_id, status, log_status, created_at, seed, pe, pp, file_groups, mp, zipped_output
            )
            VALUES (?, ?, 'queued'::simulation_status_type, 'not_ready' ::log_status_type, ?, ?, ?, ?, ?, ?, ?)
            RETURNING id
        """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, username)
                stmt.setLong(2, configId)
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()))
                stmt.setObject(4, params.seed)
                stmt.setObject(5, params.pE)
                stmt.setObject(6, params.pP)
                stmt.setObject(7, params.fileGroups)
                stmt.setObject(8, params.mp)
                stmt.setBoolean(9, params.zippedOutput)

                val rs = stmt.executeQuery()
                rs.next()
                return rs.getLong("id")
            }
        }
    }

    fun updateSimulationStatus(simulationId: Long, status: String) {
        val sql = """
            UPDATE simulation
            SET status = ? ::simulation_status_type
            WHERE id = ?
        """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, status)
                stmt.setLong(2, simulationId)
                stmt.executeUpdate()
            }
        }
    }

    fun updateLogStatus(simulationId: Long, logStatus: String) {
        val sql = """
            UPDATE simulation
            SET log_status = ? ::log_status_type
            WHERE id = ?
        """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, logStatus)
                stmt.setLong(2, simulationId)
                stmt.executeUpdate()
            }
        }
    }

    fun createSimulationLog(simulationId: Long, logUrl: String, fileSize: Long?) {
        val sql = """
            INSERT INTO simulation_log (simulation_id, log_url, file_size)
            VALUES (?, ?, ?)
        """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setLong(1, simulationId)
                stmt.setString(2, logUrl)
                stmt.setObject(3, fileSize)
                stmt.executeUpdate()
            }
        }
    }

    fun markSimulationStarted(simulationId: Long) {
        val sql = """
        UPDATE simulation
        SET status = 'running' ::simulation_status_type, started_at = ?
        WHERE id = ?
    """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()))
                stmt.setLong(2, simulationId)
                stmt.executeUpdate()
            }
        }
    }

    fun markSimulationFinished(simulationId: Long, status: String) {
        val sql = """
        UPDATE simulation
        SET status = ? ::simulation_status_type, finished_at = ?
        WHERE id = ?
    """.trimIndent()

        Database.getConnection().use { connection ->
            connection.prepareStatement(sql).use { stmt ->
                stmt.setString(1, status)
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                stmt.setLong(3, simulationId)
                stmt.executeUpdate()
            }
        }
    }

    fun getOrCreateSimulationConfig(params: SimulatorParams): Long {
        val existingId = findSimulationConfig(params)
        if (existingId != null) {
            return existingId
        }

        return createSimulationConfig(params)
    }
}

