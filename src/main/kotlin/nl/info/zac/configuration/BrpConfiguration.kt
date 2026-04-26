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

@Suppress("LongParameterList", "TooManyFunctions")
@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrpConfiguration @Inject constructor(
    @ConfigProperty(name = "BRP_PROTOCOLLERING_ENABLED", defaultValue = "false")
    private val protocolleringEnabled: Boolean,

    @ConfigProperty(name = ENV_VAR_BRP_ORIGIN_OIN)
    private val originOIN: Optional<String>,

    @ConfigProperty(name = "BRP_DOELBINDING_PER_ZAAKTYPE", defaultValue = "false")
    private val doelbindingPerZaaktype: Boolean,

    // Header name env vars — no defaultValue; Helm provides defaults.
    // An empty string disables that header.
    @ConfigProperty(name = ENV_VAR_BRP_ORIGIN_OIN_HEADER)
    private val headerNameOriginOin: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_DOELBINDING_HEADER)
    private val headerNameDoelbinding: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_DOELBINDING_ZOEKMET)
    private val doelbindingZoekMetDefault: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET)
    private val doelbindingRaadpleegMetDefault: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_VERWERKING_HEADER)
    private val headerNameVerwerking: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_VERWERKINGSREGISTER)
    private val verwerkingsregister: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_GEBRUIKER_HEADER)
    private val headerNameGebruiker: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_TOEPASSING_HEADER)
    private val headerNameToepassing: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_TOEPASSING)
    private val toepassingValue: Optional<String>,
) {
    companion object {
        const val ENV_VAR_BRP_ORIGIN_OIN = "BRP_ORIGIN_OIN"
        const val ENV_VAR_BRP_ORIGIN_OIN_HEADER = "BRP_ORIGIN_OIN_HEADER"
        const val ENV_VAR_BRP_DOELBINDING_HEADER = "BRP_DOELBINDING_HEADER"
        const val ENV_VAR_BRP_DOELBINDING_ZOEKMET = "BRP_DOELBINDING_ZOEKMET"
        const val ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET = "BRP_DOELBINDING_RAADPLEEGMET"
        const val ENV_VAR_BRP_VERWERKING_HEADER = "BRP_VERWERKING_HEADER"
        const val ENV_VAR_BRP_VERWERKINGSREGISTER = "BRP_VERWERKINGSREGISTER"
        const val ENV_VAR_BRP_GEBRUIKER_HEADER = "BRP_GEBRUIKER_HEADER"
        const val ENV_VAR_BRP_TOEPASSING_HEADER = "BRP_TOEPASSING_HEADER"
        const val ENV_VAR_BRP_TOEPASSING = "BRP_TOEPASSING"
    }

    @PostConstruct
    fun validateConfiguration() {
        if (isBrpProtocolleringEnabled()) {
            if (getHeaderNameDoelbinding() != null) {
                throwIf(!doelbindingZoekMetDefault.isPresent) {
                    "BRP_DOELBINDING_ZOEKMET environment variable is required when BRP_DOELBINDING_HEADER is set"
                }
                throwIf(!doelbindingRaadpleegMetDefault.isPresent) {
                    "BRP_DOELBINDING_RAADPLEEGMET environment variable is required when BRP_DOELBINDING_HEADER is set"
                }
            }
            if (getHeaderNameVerwerking() != null) {
                throwIf(!verwerkingsregister.isPresent) {
                    "BRP_VERWERKINGSREGISTER environment variable is required when BRP_VERWERKING_HEADER is set"
                }
            }
            if (getHeaderNameToepassing() != null) {
                throwIf(!toepassingValue.isPresent) {
                    "BRP_TOEPASSING environment variable is required when BRP_TOEPASSING_HEADER is set"
                }
            }
        }
    }

    fun getOriginOIN() = originOIN.getOrNull()

    fun getDoelbindingZoekMetDefault() = doelbindingZoekMetDefault.getOrNull()

    fun getDoelbindingRaadpleegMetDefault() = doelbindingRaadpleegMetDefault.getOrNull()

    fun getVerwerkingsRegister() = verwerkingsregister.getOrNull()

    fun getHeaderNameDoelbinding() = headerNameDoelbinding.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

    fun getHeaderNameVerwerking() = headerNameVerwerking.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

    fun getHeaderNameOriginOin() = headerNameOriginOin.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

    fun getHeaderNameGebruiker() = headerNameGebruiker.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

    fun getHeaderNameToepassing() = headerNameToepassing.getOrNull()?.trim()?.takeIf { it.isNotBlank() }

    fun getToepassing() = toepassingValue.getOrNull()

    override fun toString() = """
        |- BRP_PROTOCOLLERING_ENABLED: '$protocolleringEnabled'
        |- $ENV_VAR_BRP_ORIGIN_OIN: '${originOIN.getOrNull()}'
        |- BRP_DOELBINDING_PER_ZAAKTYPE: '$doelbindingPerZaaktype'
        |- $ENV_VAR_BRP_ORIGIN_OIN_HEADER: '${getHeaderNameOriginOin()}'
        |- $ENV_VAR_BRP_DOELBINDING_HEADER: '${getHeaderNameDoelbinding()}'
        |- $ENV_VAR_BRP_DOELBINDING_ZOEKMET: '${getDoelbindingZoekMetDefault()}'
        |- $ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET: '${getDoelbindingRaadpleegMetDefault()}'
        |- $ENV_VAR_BRP_VERWERKING_HEADER: '${getHeaderNameVerwerking()}'
        |- $ENV_VAR_BRP_VERWERKINGSREGISTER: '${getVerwerkingsRegister()}'
        |- $ENV_VAR_BRP_GEBRUIKER_HEADER: '${getHeaderNameGebruiker()}'
        |- $ENV_VAR_BRP_TOEPASSING_HEADER: '${getHeaderNameToepassing()}'
        |- $ENV_VAR_BRP_TOEPASSING: '${getToepassing()}'
    """.trimMargin()

    fun isBrpProtocolleringEnabled(): Boolean = protocolleringEnabled

    fun isDoelbindingPerZaaktype(): Boolean = doelbindingPerZaaktype && getHeaderNameDoelbinding() != null

    private inline fun throwIf(throwCondition: Boolean, messageProvider: () -> String) {
        if (throwCondition) throw BrpProtocolleringConfigurationException(messageProvider())
    }
}
