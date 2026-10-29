/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.cache

import jakarta.annotation.PreDestroy
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.interceptor.Interceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.authentication.runAsSystemUser
import nl.info.zac.configuration.ConfigurationService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.time.measureTime

@ApplicationScoped
@AllOpen
@NoArgConstructor
class ZtcCacheRefresher @Inject constructor(
    private val ztcClientService: ZtcClientService,
    private val configurationService: ConfigurationService,

    /**
     * Declare a Kotlin coroutine dispatcher here so that it can be overridden in unit tests with a test dispatcher
     * while in normal operation it will be injected using [nl.info.zac.util.CoroutineDispatcherProducer].
     */
    dispatcher: CoroutineDispatcher
) {
    companion object {
        /**
         * Later than the default observer priority of [ConfigurationService.onStartup], which reads the default
         * catalogus that the refresh needs.
         */
        private const val STARTUP_OBSERVER_PRIORITY = Interceptor.Priority.APPLICATION + 600

        private val LOG = Logger.getLogger(ZtcCacheRefresher::class.java.name)
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        LOG.log(Level.SEVERE, "Unexpected failure while refreshing the ZTC caches", throwable)
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + dispatcher + exceptionHandler)

    fun onStartup(
        @Observes @Initialized(ApplicationScoped::class) @Priority(STARTUP_OBSERVER_PRIORITY)
        @Suppress("UNUSED_PARAMETER")
        event: Any
    ) {
        coroutineScope.launch { refreshCaches() }
    }

    @PreDestroy
    fun shutdown() = coroutineScope.cancel()

    private fun refreshCaches() {
        LOG.info("Started refreshing the ZTC caches")
        val duration = measureTime {
            runAsSystemUser { ztcClientService.refreshCaches(configurationService.readDefaultCatalogusURI()) }
        }
        LOG.info { "Finished refreshing the ZTC caches in $duration" }
    }
}
