/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.time.Duration

private const val POSTGRES_IMAGE = "postgres:17.11"
private const val FLYWAY_IMAGE = "flyway/flyway"
private const val DATABASE_NAME = "zac"
private const val DATABASE_USER = "zac"
private const val DATABASE_PASSWORD = "zac"
private const val NULL_VALUE = "<null>"
private const val COLUMN_SEPARATOR = "\t"
private val FLYWAY_TIMEOUT = Duration.ofMinutes(5)
const val ZAC_SCHEMA = "zaakafhandelcomponent"

/**
 * An empty PostgreSQL database, separate from the ZAC Docker Compose stack, on which a test runs the ZAC Flyway
 * migrations up to a chosen version, seeds data in the schema of that version, and migrates further.
 * Flyway runs in its own container, with the Flyway version that ZAC uses.
 */
class MigrationTestDatabase : AutoCloseable {
    private val postgresContainer = GenericContainer<Nothing>(DockerImageName.parse(POSTGRES_IMAGE)).apply {
        withEnv("POSTGRES_DB", DATABASE_NAME)
        withEnv("POSTGRES_USER", DATABASE_USER)
        withEnv("POSTGRES_PASSWORD", DATABASE_PASSWORD)
        waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*", 2))
        start()
    }

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

    /**
     * Migrates the database to the given version and returns the Flyway output.
     */
    fun migrateTo(version: String): String {
        val flywayVersion = checkNotNull(System.getProperty("flywayVersion")) {
            "System property 'flywayVersion' is not set"
        }
        val flywayContainer = GenericContainer<Nothing>(DockerImageName.parse("$FLYWAY_IMAGE:$flywayVersion")).apply {
            // shares the network namespace of the database container, so that Flyway reaches it on localhost
            withNetworkMode("container:${postgresContainer.containerId}")
            withCopyFileToContainer(MountableFile.forHostPath("src/main/resources/schemas"), "/flyway/sql")
            withCommand(
                "-url=jdbc:postgresql://localhost:5432/$DATABASE_NAME",
                "-user=$DATABASE_USER",
                "-password=$DATABASE_PASSWORD",
                "-schemas=$ZAC_SCHEMA",
                "-placeholders.schema=$ZAC_SCHEMA",
                "-locations=filesystem:/flyway/sql",
                "-target=$version",
                "migrate"
            )
            withStartupCheckStrategy(OneShotStartupCheckStrategy().withTimeout(FLYWAY_TIMEOUT))
        }
        return flywayContainer.use {
            try {
                it.start()
            } catch (containerLaunchException: ContainerLaunchException) {
                throw IllegalStateException(
                    "Flyway failed to migrate to version $version: ${it.logs}",
                    containerLaunchException
                )
            }
            it.logs
        }
    }

    fun execute(sql: String) {
        runPsql(sql)
    }

    /**
     * Returns every row of the query as a list of its column values, with SQL NULL as `null`.
     */
    fun query(sql: String): List<List<String?>> =
        runPsql(sql).lines()
            .filter { it.isNotEmpty() }
            .map { row -> row.split(COLUMN_SEPARATOR).map { it.takeUnless { value -> value == NULL_VALUE } } }

    private fun runPsql(sql: String): String {
        val execResult = postgresContainer.execInContainer(
            "psql",
            "--username=$DATABASE_USER",
            "--dbname=$DATABASE_NAME",
            "--set=ON_ERROR_STOP=1",
            "--no-align",
            "--tuples-only",
            "--quiet",
            "--field-separator=$COLUMN_SEPARATOR",
            "--pset=null=$NULL_VALUE",
            "--command=SET search_path TO $ZAC_SCHEMA, public",
            "--command=$sql"
        )
        check(execResult.exitCode == 0) { "psql failed: ${execResult.stderr}" }
        return execResult.stdout
    }

    override fun close() = postgresContainer.stop()
}
