/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuratie

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional
import java.util.logging.Logger
import kotlin.jvm.optionals.getOrDefault

@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpConfiguration @Inject constructor(
    @ConfigProperty(name = "BRP_API_KEY")
    val apiKey: Optional<String>,

    @ConfigProperty(name = "BRP_ORIGIN_OIN")
    val originOIN: Optional<String>,

    @ConfigProperty(name = "BRP_PROTOCOLERING")
    val auditLogProvider: Optional<String>,

    @ConfigProperty(name = "BRP_DOELBINDING_ZOEKMET")
    val queryPersonenDefaultPurpose: Optional<String>,

    @ConfigProperty(name = "BRP_DOELBINDING_RAADPLEEGMET")
    val retrievePersoonDefaultPurpose: Optional<String>,

    @ConfigProperty(name = "BRP_VERWERKINGSREGISTER")
    val processingRegisterDefault: Optional<String>
) {
    companion object {
        private val LOG = Logger.getLogger(BrpConfiguration::class.java.name)
        val SUPPORTED_AUDIT_LOG_PROVIDERS = arrayOf("iConnect", "2Secure")
        const val DEFAULT_AUDIT_LOG_PROVIDER = "iConnect"
    }

    fun isBrpProtocoleringEnabled(): Boolean = originOIN.isPresent

    fun readBrpAuditLogProvider(): String =
        auditLogProvider.getOrDefault(DEFAULT_AUDIT_LOG_PROVIDER)
            .takeIf { it in SUPPORTED_AUDIT_LOG_PROVIDERS }
            ?: DEFAULT_AUDIT_LOG_PROVIDER.also {
                LOG.warning("Invalid BRP audit log provider '$it', defaulting to '$DEFAULT_AUDIT_LOG_PROVIDER'")
            }
}
