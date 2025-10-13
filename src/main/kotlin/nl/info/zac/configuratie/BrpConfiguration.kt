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
import kotlin.jvm.optionals.getOrNull

@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpConfiguration @Inject constructor(
    @ConfigProperty(name = "BRP_API_KEY")
    val apiKey: Optional<String>,

    @ConfigProperty(name = "BRP_ORIGIN_OIN")
    val originOIN: Optional<String>,

    @ConfigProperty(name = "BRP_PROTOCOLLERING")
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
        val SUPPORTED_PROTOCOLLERING_PROVIDERS = arrayOf("iConnect", "2Secure")
    }

    fun isBrpProtocolleringEnabled(): Boolean = originOIN.isPresent

    fun readBrpProtocolleringProvider(): String {
        LOG.warning { "BRP_PROTOCOLLERING environment variable value: ${auditLogProvider.getOrNull()}" }
        LOG.warning { "Environment variables: ${System.getenv().entries.joinToString { "${it.key}: ${it.value}" }}" }
        return requireNotNull(auditLogProvider.getOrNull().takeIf { it in SUPPORTED_PROTOCOLLERING_PROVIDERS }) {
            if (auditLogProvider.isPresent) {
                "Invalid environment variable 'BRP_PROTOCOLLERING' value '$auditLogProvider'. Supported: " + SUPPORTED_PROTOCOLLERING_PROVIDERS.joinToString()
            } else {
                "Missing environment variable 'BRP_PROTOCOLLERING'. Supported: " + SUPPORTED_PROTOCOLLERING_PROVIDERS.joinToString()
            }
        }
    }
}
