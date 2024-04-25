package net.atos.zac.configuration

import jakarta.servlet.ServletContextEvent
import jakarta.servlet.ServletContextListener
import jakarta.servlet.annotation.WebListener
import net.atos.zac.zaken.ZakenService
import java.util.logging.Logger

@WebListener
class ConfigurationServletContextListener : ServletContextListener {
    companion object {
        private val LOG = Logger.getLogger(ZakenService::class.java.name)
    }

    /**
     * Logs the configured ZAC environment variables.
     * Take care not to log values of environment variables which contain sensitive information.
     */
    override fun contextInitialized(servletContextEvent: ServletContextEvent) {
        LOG.info("Servlet context initialized")
        LOG.info("Configured ZAC environment variables:")
        ZAC_ENVIRONMENT_VARIABLES.forEach {
            LOG.info("   $it = ${System.getenv(it)}")
        }
    }

    override fun contextDestroyed(servletContextEvent: ServletContextEvent) {
        // no operation
    }
}
