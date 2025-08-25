/*
* SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
* SPDX-License-Identifier: EUPL-1.2+
*/
package net.atos.zac.util

import jakarta.annotation.Resource
import jakarta.ejb.TransactionManagement
import jakarta.ejb.TransactionManagementType.BEAN
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import nl.info.zac.database.flyway.exception.DatabaseConfigurationException
import org.flywaydb.core.Flyway
import java.util.logging.Logger
import javax.sql.DataSource

/**
 * See [Integrating Flyway with Java EE and using its datasource]
 * (http://are-you-ready.de/integrating-flyway-with-java-ee-and-using-its-datasource/)
 */
@TransactionManagement(BEAN)
class FlywayIntegrator {
    companion object {
        const val SCHEMA = "zaakafhandelcomponent"
        private val LOG = Logger.getLogger(FlywayIntegrator::class.java.name)
        private const val SCHEMA_FILES_LOCATION = "schemas"
        private const val SCHEMA_PLACEHOLDER = "schema"
    }

    @Resource(lookup = "java:comp/env/jdbc/Datasource")
    private lateinit var dataSource: DataSource

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        if (!::dataSource.isInitialized) {
            throw DatabaseConfigurationException("No data source found to execute the ZAC database migrations")
        }
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(SCHEMA_FILES_LOCATION)
            .schemas(SCHEMA)
            .placeholders(mapOf(SCHEMA_PLACEHOLDER to SCHEMA))
            .outOfOrder(true)
            .load()
        flyway.info().current()?.let {
            LOG.info("Found existing ZAC database: version '${it.version}', description: '${it.description}'")
        } ?: LOG.info("No existing ZAC database at the configured datasource")

        flyway.migrate()
        LOG.info("Successfully migrated to database version: '${flyway.info().current().version}'")
    }
}
