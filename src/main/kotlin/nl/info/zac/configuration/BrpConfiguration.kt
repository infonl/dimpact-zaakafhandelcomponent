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
import java.util.logging.Level
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

class BrpConfigurationValueImpl(
    private val envVariable: String,
    private val maxSize: Int,
    private val headerName: Optional<String>,
    private val valueSupplier: () -> String?,
    private val defaultValueSupplier: () -> String? = { null }
) : BrpConfigurationValue {
    override fun isAvailable() = headerName.isPresent && (headerName.getOrNull()?.isNotBlank() ?: false)

    override fun getHeaderName() =
        if (isAvailable()) {
            headerName.get()
        } else {
            throw BrpProtocolleringConfigurationException(
                "HeaderName is required. Environment variable '$envVariable' has to be set."
            )
        }

    override fun getValue(): String? =
        if (isAvailable()) {
            valueSupplier() ?: defaultValueSupplier()
        } else {
            null
        }?.take(maxSize)
}

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

    @ConfigProperty(name = ENV_VAR_BRP_SYSTEM_USER)
    private val systemUser: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_LOG_LEVEL)
    private val logLevel: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_API_KEY_HEADER)
    private val headerNameApiKey: Optional<String>,

    @ConfigProperty(name = ENV_VAR_BRP_API_KEY)
    private val apiKey: Optional<String>,
) : BrpConfigurationProvider {
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
        const val ENV_VAR_BRP_SYSTEM_USER = "BRP_SYSTEM_USER"
        const val ENV_VAR_BRP_LOG_LEVEL = "BRP_LOG_LEVEL"
        const val ENV_VAR_BRP_API_KEY_HEADER = "BRP_API_KEY_HEADER"
        const val ENV_VAR_BRP_API_KEY = "BRP_API_KEY"

        // Audit log headers are limited to 242 characters as this is the maximum length of the DB columns:
        // https://github.com/VNG-Realisatie/gemma-verwerkingenlogging/blob/002df5b01bf7d10142c9ae042a041b096989ced9/docs/api-write/oas-specification/logging-verwerkingen-api/openapi.yaml#L1170-L1175
        const val MAX_HEADER_SIZE = 242

        // User headers are limited to 40 characters as this is the maximum length of the DB column:
        // https://github.com/VNG-Realisatie/gemma-verwerkingenlogging/blob/002df5b01bf7d10142c9ae042a041b096989ced9/docs/api-write/oas-specification/logging-verwerkingen-api/openapi.yaml#L1224-L1229
        const val MAX_USER_HEADER_SIZE = 40
    }

    @PostConstruct
    fun validateConfiguration() {
        if (isBrpProtocolleringEnabled()) {
            if (headerNameDoelbinding.isPresentNotBlank()) {
                doelbindingZoekMetDefault.isEmptyOrBlank().thenThrow {
                    "BRP_DOELBINDING_ZOEKMET environment variable is required when BRP_DOELBINDING_HEADER is set"
                }
                (
                    !doelbindingRaadpleegMetDefault.isPresent || doelbindingRaadpleegMetDefault.get()
                        .isBlank()
                    ).thenThrow {
                    "BRP_DOELBINDING_RAADPLEEGMET environment variable is required when BRP_DOELBINDING_HEADER is set"
                }
            }
            if (headerNameVerwerking.isPresentNotBlank()) {
                verwerkingsregister.isEmptyOrBlank().thenThrow {
                    "BRP_VERWERKINGSREGISTER environment variable is required when BRP_VERWERKING_HEADER is set"
                }
            }
            if (headerNameToepassing.isPresentNotBlank()) {
                toepassingValue.isEmptyOrBlank().thenThrow {
                    "BRP_TOEPASSING environment variable is required when BRP_TOEPASSING_HEADER is set"
                }
            }
        }
    }

    override fun getLogLevel(): Level = Level.parse(logLevel.getOrElse { "OFF" }) ?: Level.OFF

    override fun getOriginOIN() =
        BrpConfigurationValueImpl(ENV_VAR_BRP_ORIGIN_OIN, MAX_HEADER_SIZE, headerNameOriginOin, originOIN::getOrNull)

    override fun getDoelbindingZoekMetDefault() =
        buildDoelbindingConfig(ENV_VAR_BRP_DOELBINDING_ZOEKMET, doelbindingZoekMetDefault::getOrNull)

    override fun getDoelbindingRaadpleegMetDefault() =
        buildDoelbindingConfig(ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET, doelbindingRaadpleegMetDefault::getOrNull)

    override fun buildDoelbinding(doelbindingSupplier: () -> String?) =
        BrpConfigurationValueImpl(
            ENV_VAR_BRP_DOELBINDING_HEADER,
            MAX_USER_HEADER_SIZE,
            headerNameDoelbinding,
            doelbindingSupplier
        )

    private fun buildDoelbindingConfig(envVariable: String, doelbindingSupplier: () -> String?) =
        BrpConfigurationValueImpl(envVariable, MAX_HEADER_SIZE, headerNameDoelbinding, doelbindingSupplier)

    override fun getVerwerkingRegisterDefault() =
        buildVerwerkingRegisterConfig(ENV_VAR_BRP_VERWERKINGSREGISTER, verwerkingsregister::getOrNull)

    override fun buildVerwerkingRegister(verwerkingSupplier: () -> String?) =
        buildVerwerkingRegisterConfig(ENV_VAR_BRP_VERWERKING_HEADER, verwerkingSupplier)

    private fun buildVerwerkingRegisterConfig(envVariable: String, verwerkingSupplier: () -> String?) =
        BrpConfigurationValueImpl(
            envVariable,
            MAX_HEADER_SIZE,
            headerNameVerwerking,
            verwerkingSupplier,
            verwerkingsregister::getOrNull
        )

    override fun getToepassing() =
        BrpConfigurationValueImpl(
            ENV_VAR_BRP_TOEPASSING_HEADER,
            MAX_HEADER_SIZE,
            headerNameToepassing,
            toepassingValue::getOrNull
        )

    override fun getApiKey() =
        BrpConfigurationValueImpl(
            ENV_VAR_BRP_API_KEY_HEADER,
            Int.MAX_VALUE,
            headerNameApiKey,
            apiKey::getOrNull
        )

    override fun buildUser(userSupplier: () -> String?) =
        BrpConfigurationValueImpl(
            ENV_VAR_BRP_GEBRUIKER_HEADER,
            MAX_USER_HEADER_SIZE,
            headerNameGebruiker,
            userSupplier,
            systemUser::getOrNull
        )

    override fun toString() = """
        |- BRP_PROTOCOLLERING_ENABLED: '$protocolleringEnabled'
        |- $ENV_VAR_BRP_ORIGIN_OIN: '${originOIN.getOrNull()}'
        |- BRP_DOELBINDING_PER_ZAAKTYPE: '$doelbindingPerZaaktype'
        |- $ENV_VAR_BRP_ORIGIN_OIN_HEADER: '${headerNameOriginOin.getOrNull()}'
        |- $ENV_VAR_BRP_DOELBINDING_HEADER: '${headerNameDoelbinding.getOrNull()}'
        |- $ENV_VAR_BRP_DOELBINDING_ZOEKMET: '${doelbindingZoekMetDefault.getOrNull()}'
        |- $ENV_VAR_BRP_DOELBINDING_RAADPLEEGMET: '${doelbindingRaadpleegMetDefault.getOrNull()}'
        |- $ENV_VAR_BRP_VERWERKING_HEADER: '${headerNameVerwerking.getOrNull()}'
        |- $ENV_VAR_BRP_VERWERKINGSREGISTER: '${verwerkingsregister.getOrNull()}'
        |- $ENV_VAR_BRP_GEBRUIKER_HEADER: '${headerNameGebruiker.getOrNull()}'
        |- $ENV_VAR_BRP_TOEPASSING_HEADER: '${headerNameToepassing.getOrNull()}'
        |- $ENV_VAR_BRP_TOEPASSING: '${toepassingValue.getOrNull()}'
        |- $ENV_VAR_BRP_SYSTEM_USER: '${systemUser.getOrNull()}'
        |- $ENV_VAR_BRP_API_KEY_HEADER: '${headerNameApiKey.getOrNull()}'
        |- $ENV_VAR_BRP_API_KEY: [REDACTED]
    """.trimMargin()

    override fun isBrpProtocolleringEnabled(): Boolean = protocolleringEnabled

    override fun isDoelbindingPerZaaktype(): Boolean =
        doelbindingPerZaaktype && headerNameDoelbinding.isPresentNotBlank()

    override fun getHeaderUser(): String? = headerNameGebruiker.getOrNull()

    private fun Optional<String>.isEmptyOrBlank(): Boolean = this.isEmpty || get().isBlank()

    private fun Optional<String>.isPresentNotBlank(): Boolean = !this.isEmptyOrBlank()

    private inline fun Boolean.thenThrow(messageProvider: () -> String) {
        if (this) throw BrpProtocolleringConfigurationException(messageProvider())
    }
}
