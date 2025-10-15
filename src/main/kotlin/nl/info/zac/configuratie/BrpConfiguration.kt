/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuratie

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.zac.configuratie.exception.BrpProtocolleringProviderNotFound
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional
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
        val SUPPORTED_PROTOCOLLERING_PROVIDERS = arrayOf("iConnect", "2Secure")
    }

    fun isBrpProtocolleringEnabled(): Boolean = originOIN.isPresent

    fun readBrpProtocolleringProvider(): String =
        auditLogProvider.getOrNull().takeIf { it in SUPPORTED_PROTOCOLLERING_PROVIDERS }
            ?: throw BrpProtocolleringProviderNotFound(
                SUPPORTED_PROTOCOLLERING_PROVIDERS.joinToString().let {
                    if (auditLogProvider.isPresent) {
                        "Invalid environment variable 'BRP_PROTOCOLLERING' value '$auditLogProvider'. Supported: $it"
                    } else {
                        "Missing environment variable 'BRP_PROTOCOLLERING'. Supported: $it"
                    }
                }
            )
}
