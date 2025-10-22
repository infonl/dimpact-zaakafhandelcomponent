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
    val doelbindingZoekMetDefault: Optional<String>,

    @ConfigProperty(name = "BRP_DOELBINDING_RAADPLEEGMET")
    val doelbindingRaadpleegMetDefault: Optional<String>,

    @ConfigProperty(name = "BRP_VERWERKINGSREGISTER")
    val verwerkingregisterDefault: Optional<String>
) {
    companion object {
        val SUPPORTED_PROTOCOLLERING_PROVIDERS = arrayOf("iConnect", "2Secure")
    }

    init {
        if (isBrpProtocolleringEnabled()) {
            check(
                doelbindingZoekMetDefault.isPresent
            ) { "BRP_DOELBINDING_ZOEKMET environment variable is required when BRP_ORIGIN_OIN is set" }
            check(
                doelbindingRaadpleegMetDefault.isPresent
            ) { "BRP_DOELBINDING_RAADPLEEGMET environment variable is required when BRP_ORIGIN_OIN is set" }
            check(
                verwerkingregisterDefault.isPresent
            ) { "BRP_VERWERKINGSREGISTER environment variable is required when BRP_ORIGIN_OIN is set" }
            check(
                auditLogProvider.isPresent
            ) { "BRP_PROTOCOLLERING environment variable is required when BRP_ORIGIN_OIN is set" }
            checkNotNull(auditLogProvider.getOrNull().takeIf { it in SUPPORTED_PROTOCOLLERING_PROVIDERS }) {
                SUPPORTED_PROTOCOLLERING_PROVIDERS.joinToString().let {
                    "Invalid environment variable 'BRP_PROTOCOLLERING' value '$auditLogProvider'. Supported: $it"
                }
            }
        }
    }

    fun isBrpProtocolleringEnabled(): Boolean = originOIN.isPresent

    fun readBrpProtocolleringProvider(): String =
        if (isBrpProtocolleringEnabled()) {
            auditLogProvider.get()
        } else {
            ""
        }
}
