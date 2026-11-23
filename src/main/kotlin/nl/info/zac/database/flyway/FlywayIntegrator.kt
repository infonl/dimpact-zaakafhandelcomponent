/*
* SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
* SPDX-License-Identifier: EUPL-1.2+
*/
package nl.info.zac.database.flyway

import jakarta.annotation.Resource
import jakarta.ejb.TransactionManagement
import jakarta.ejb.TransactionManagementType
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import nl.info.zac.database.flyway.exception.DatabaseConfigurationException
import nl.info.zac.database.flyway.log.DatabaseMigrated
import nl.info.zac.log.ZacLogger
import org.flywaydb.core.Flyway
import javax.sql.DataSource

/**
 * See [Integrating Flyway with Java EE and using its datasource]
 * (http://are-you-ready.de/integrating-flyway-with-java-ee-and-using-its-datasource/)
 */
@TransactionManagement(TransactionManagementType.BEAN)
class FlywayIntegrator {
    companion object {
        const val SCHEMA = "zaakafhandelcomponent"
        private val LOG = ZacLogger.getLogger(FlywayIntegrator::class)
        private const val SCHEMA_FILES_LOCATION = "schemas"
        private const val SCHEMA_PLACEHOLDER = "schema"
    }

    @Resource(lookup = "java:comp/env/jdbc/Datasource")
    lateinit var dataSource: DataSource

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) {
        if (!this::dataSource.isInitialized) {
            throw DatabaseConfigurationException("No data source found to execute the ZAC database migrations")
        }
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations(SCHEMA_FILES_LOCATION)
            .schemas(SCHEMA)
            .placeholders(mapOf(SCHEMA_PLACEHOLDER to SCHEMA))
            .outOfOrder(false)
            .load()
        val previousMigration = flyway.info().current()
        flyway.migrate()
        LOG.info(
            DatabaseMigrated(
                previousVersion = previousMigration?.version?.toString(),
                previousDescription = previousMigration?.description,
                currentVersion = flyway.info().current()?.version?.toString()
            )
        )
    }
}
