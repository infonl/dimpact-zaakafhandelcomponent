/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import org.flywaydb.core.Flyway
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import java.sql.DriverManager

private const val POSTGRES_IMAGE = "postgres:17.11"
private const val POSTGRES_PORT = 5432
private const val DATABASE_NAME = "zac"
private const val DATABASE_USER = "zac"
private const val DATABASE_PASSWORD = "zac"
const val ZAC_SCHEMA = "zaakafhandelcomponent"

/**
 * An empty PostgreSQL database, separate from the ZAC Docker Compose stack, on which a test runs the ZAC Flyway
 * migrations up to a chosen version, seeds data in the schema of that version, and migrates further.
 */
class MigrationTestDatabase : AutoCloseable {
    private val postgresContainer = GenericContainer<Nothing>(DockerImageName.parse(POSTGRES_IMAGE)).apply {
        withEnv("POSTGRES_DB", DATABASE_NAME)
        withEnv("POSTGRES_USER", DATABASE_USER)
        withEnv("POSTGRES_PASSWORD", DATABASE_PASSWORD)
        withExposedPorts(POSTGRES_PORT)
        waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
        start()
    }

    private val jdbcUrl =
        "jdbc:postgresql://${postgresContainer.host}:${postgresContainer.getMappedPort(POSTGRES_PORT)}/$DATABASE_NAME"

    init {
        // V90 reads the BPMN process definitions that Flowable stores in its own schema.
        execute(
            """
            CREATE SCHEMA flowable;
            CREATE TABLE flowable.act_re_procdef (key_ VARCHAR, version_ INTEGER, deployment_id_ VARCHAR);
            CREATE TABLE flowable.act_ge_bytearray (name_ VARCHAR, deployment_id_ VARCHAR, bytes_ BYTEA);
            """.trimIndent()
        )
    }

    fun migrateTo(version: String) {
        Flyway.configure()
            .dataSource(jdbcUrl, DATABASE_USER, DATABASE_PASSWORD)
            .locations("filesystem:src/main/resources/schemas")
            .schemas(ZAC_SCHEMA)
            .placeholders(mapOf("schema" to ZAC_SCHEMA))
            .target(version)
            .load()
            .migrate()
    }

    fun execute(sql: String) {
        DriverManager.getConnection(jdbcUrl, DATABASE_USER, DATABASE_PASSWORD).use { connection ->
            connection.createStatement().use { it.execute("SET search_path TO $ZAC_SCHEMA, public; $sql") }
        }
    }

    /**
     * Returns every row of the query as a list of its column values, converted to strings.
     */
    fun query(sql: String): List<List<String?>> =
        DriverManager.getConnection(jdbcUrl, DATABASE_USER, DATABASE_PASSWORD).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("SET search_path TO $ZAC_SCHEMA, public")
                statement.executeQuery(sql).use { resultSet ->
                    val columnCount = resultSet.metaData.columnCount
                    buildList {
                        while (resultSet.next()) {
                            add((1..columnCount).map { resultSet.getString(it) })
                        }
                    }
                }
            }
        }

    override fun close() = postgresContainer.stop()
}
