/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import nl.info.zac.configuration.exception.BrpProtocolleringConfigurationException
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpConfiguration @Inject constructor(
    @ConfigProperty(name = ENV_VAR_BRP_API_KEY)
    private val apiKey: Optional<String>,

    /**
     * OIN of the originator, which is required for BRP protocollering.
     * If this variable is not set, BRP protocollering will be disabled.
     */
    @ConfigProperty(name = ENV_VAR_BRP_ORIGIN_OIN)
    private val originOIN: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_PROTOCOLLERING_PROVIDER)
    private val brpProtocolleringProvider: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_DOELBINDING_ZOEKMET)
    private val doelbindingZoekMetDefault: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET)
    private val doelbindingRaadpleegMetDefault: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_VERWERKINGSREGISTER)
    private val verwerkingsregister: Optional<String>
) {
    companion object {
        const val ENV_VAR_BRP_API_KEY = "BRP_API_KEY"
        const val ENV_VAR_BRP_ORIGIN_OIN = "BRP_ORIGIN_OIN"
        const val ENV_VAR_BRP_PROTOCOLLERING_PROVIDER = "BRP_PROTOCOLLERING"
        const val ENV_VAR_BRP_DOELBINDING_ZOEKMET = "BRP_DOELBINDING_ZOEKMET"
        const val ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET = "BRP_DOELBINDING_RAADPLEEGMET"
        const val ENV_VAR_BRP_VERWERKINGSREGISTER = "BRP_VERWERKINGSREGISTER"
        const val BRP_PROTOCOLLERING_PROVIDER_2SECURE = "2Secure"
        const val BRP_PROTOCOLLERING_PROVIDER_ICONNECT = "iConnect"
        val SUPPORTED_PROTOCOLLERING_PROVIDERS =
            arrayOf(BRP_PROTOCOLLERING_PROVIDER_ICONNECT, BRP_PROTOCOLLERING_PROVIDER_2SECURE)
    }

    @PostConstruct
    fun validateConfiguration() {
        if (isBrpProtocolleringEnabled()) {
            throwIf(!doelbindingZoekMetDefault.isPresent) {
                "BRP_DOELBINDING_ZOEKMET environment variable is required when BRP_ORIGIN_OIN is set"
            }
            throwIf(!doelbindingRaadpleegMetDefault.isPresent) {
                "BRP_DOELBINDING_RAADPLEEGMET environment variable is required when BRP_ORIGIN_OIN is set"
            }
            throwIf(!verwerkingsregister.isPresent) {
                "BRP_VERWERKINGSREGISTER environment variable is required when BRP_ORIGIN_OIN is set"
            }
            throwIf(!brpProtocolleringProvider.isPresent) {
                "BRP_PROTOCOLLERING environment variable is required when BRP_ORIGIN_OIN is set"
            }
            throwIf(brpProtocolleringProvider.getOrNull() !in SUPPORTED_PROTOCOLLERING_PROVIDERS) {
                SUPPORTED_PROTOCOLLERING_PROVIDERS.joinToString().let {
                    "Invalid environment variable 'BRP_PROTOCOLLERING' value '${brpProtocolleringProvider.getOrNull()}'. Supported: $it"
                }
            }
        }
    }

    fun getApiKey() = apiKey.getOrNull()

    fun getOriginOIN() = originOIN.getOrNull()

    fun getDoelbindingZoekMetDefault() = doelbindingZoekMetDefault.getOrNull()

    fun getDoelbindingRaadpleegMetDefault() = doelbindingRaadpleegMetDefault.getOrNull()

    fun getVerwerkingsRegister() = verwerkingsregister.getOrNull()

    override fun toString() =
        "$ENV_VAR_BRP_API_KEY: '" + if (apiKey.isPresent) {
            "***"
        } else {
            "null" + "', " +
                "$ENV_VAR_BRP_ORIGIN_OIN: '${originOIN.getOrNull()}', " +
                "$ENV_VAR_BRP_PROTOCOLLERING_PROVIDER: '${brpProtocolleringProvider.getOrNull()}', " +
                "$ENV_VAR_BRP_DOELBINDING_ZOEKMET: '${getDoelbindingZoekMetDefault()}', " +
                "$ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET: '${getDoelbindingRaadpleegMetDefault()}', " +
                "$ENV_VAR_BRP_VERWERKINGSREGISTER: '${getVerwerkingsRegister()}'"
        }

    fun isBrpProtocolleringEnabled(): Boolean = originOIN.isPresent

    fun readBrpProtocolleringProvider(): String =
        if (isBrpProtocolleringEnabled()) {
            brpProtocolleringProvider.get()
        } else {
            ""
        }

    private inline fun throwIf(throwCondition: Boolean, messageProvider: () -> String) {
        if (throwCondition) throw BrpProtocolleringConfigurationException(messageProvider())
    }
}
