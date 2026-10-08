/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.itest.util

import nl.info.zac.itest.config.ItestConfiguration.ZAC_DATABASE_CONTAINER_SERVICE_NAME
import nl.info.zac.itest.config.dockerComposeContainer
import org.testcontainers.containers.ContainerLaunchException
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.ToStringConsumer
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy
import org.testcontainers.utility.DockerImageName
import org.testcontainers.utility.MountableFile
import java.time.Duration
import java.util.UUID

private const val FLYWAY_IMAGE = "flyway/flyway"
private const val ZAC_DATABASE_NAME = "zac"
private const val DATABASE_USER = "zac"
private const val DATABASE_PASSWORD = "password"
private const val NULL_VALUE = "<null>"
private const val COLUMN_SEPARATOR = "\t"
private val FLYWAY_TIMEOUT = Duration.ofMinutes(5)
const val ZAC_SCHEMA = "zaakafhandelcomponent"

/**
 * An empty database in the ZAC database container of the Docker Compose stack, on which a test runs the ZAC Flyway
 * migrations up to a chosen version, seeds data in the schema of that version, and migrates further.
 * Flyway runs in its own container, with the Flyway version that ZAC uses.
 */
class MigrationTestDatabase : AutoCloseable {
    private val databaseName = "zac_migration_test_${UUID.randomUUID().toString().replace("-", "")}"
    private val zacDatabaseContainer = dockerComposeContainer
        .getContainerByServiceName(ZAC_DATABASE_CONTAINER_SERVICE_NAME)
        .get()

    init {
        runPsql(ZAC_DATABASE_NAME, "CREATE DATABASE $databaseName")
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
        val flywayOutput = ToStringConsumer()
        val flywayContainer = GenericContainer<Nothing>(DockerImageName.parse("$FLYWAY_IMAGE:$flywayVersion")).apply {
            // shares the network namespace of the database container, so that Flyway reaches it on localhost
            withNetworkMode("container:${zacDatabaseContainer.containerId}")
            withCopyFileToContainer(MountableFile.forHostPath("src/main/resources/schemas"), "/flyway/sql")
            withCommand(
                "-url=jdbc:postgresql://localhost:5432/$databaseName",
                "-user=$DATABASE_USER",
                "-password=$DATABASE_PASSWORD",
                "-schemas=$ZAC_SCHEMA",
                "-placeholders.schema=$ZAC_SCHEMA",
                "-locations=filesystem:/flyway/sql",
                "-target=$version",
                "migrate"
            )
            withStartupCheckStrategy(OneShotStartupCheckStrategy().withTimeout(FLYWAY_TIMEOUT))
            withLogConsumer(flywayOutput)
        }
        return flywayContainer.use {
            try {
                it.start()
            } catch (containerLaunchException: ContainerLaunchException) {
                throw IllegalStateException(
                    "Flyway failed to migrate to version $version: ${flywayOutput.toUtf8String()}",
                    containerLaunchException
                )
            }
            flywayOutput.toUtf8String()
        }
    }

    fun execute(sql: String) {
        runPsql(databaseName, sql)
    }

    /**
     * Returns every row of the query as a list of its column values, with SQL NULL as `null`.
     */
    fun query(sql: String): List<List<String?>> =
        runPsql(databaseName, sql).lines()
            .filter { it.isNotEmpty() }
            .map { row -> row.split(COLUMN_SEPARATOR).map { it.takeUnless { value -> value == NULL_VALUE } } }

    private fun runPsql(database: String, sql: String): String {
        val execResult = zacDatabaseContainer.execInContainer(
            "psql",
            "--username=$DATABASE_USER",
            "--dbname=$database",
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

    override fun close() {
        runPsql(ZAC_DATABASE_NAME, "DROP DATABASE $databaseName WITH (FORCE)")
    }
}
