/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp.util

import nl.info.zac.configuration.BrpConfiguration
import java.util.Optional
import java.util.UUID

@Suppress("LongParameterList")
fun createBrpConfiguration(
    protocolleringEnabled: Boolean = true,
    originOin: Optional<String> = Optional.of("fakeOriginOin"),
    doelbindingPerZaaktypeEnabled: Boolean = false,
    doelbindingZoekMetDefault: Optional<String> = Optional.of("fakeDoelbindingZoekMetDefault"),
    doelbindingRaadpleegMetDefault: Optional<String> = Optional.of("fakeDoelbindingRaadpleegMetDefault"),
    verwerkingregisterDefault: Optional<String> = Optional.of("fakeVerwerkingregisterDefault"),
    headerNameDoelbinding: Optional<String> = Optional.of("x-doelbinding"),
    headerNameVerwerking: Optional<String> = Optional.of("x-verwerking"),
    headerNameOriginOin: Optional<String> = Optional.of("x-origin-oin"),
    headerNameGebruiker: Optional<String> = Optional.of("x-gebruiker"),
    headerNameToepassing: Optional<String> = Optional.of("x-toepassing"),
    toepassingValue: Optional<String> = Optional.of("ZAC"),
    systemUser: Optional<String> = Optional.of("fakeSystemUser"),
    logLevel: Optional<String> = Optional.of("INFO"),
    apiKey: Optional<String> = Optional.of(UUID.randomUUID().toString()),
    headerNameApiKey: Optional<String> = Optional.of("x-api-key"),
) = BrpConfiguration(
    protocolleringEnabled = protocolleringEnabled,
    originOIN = originOin,
    doelbindingPerZaaktypeEnabled = doelbindingPerZaaktypeEnabled,
    doelbindingZoekMetDefault = doelbindingZoekMetDefault,
    doelbindingRaadpleegMetDefault = doelbindingRaadpleegMetDefault,
    verwerkingsregister = verwerkingregisterDefault,
    headerNameDoelbinding = headerNameDoelbinding,
    headerNameVerwerking = headerNameVerwerking,
    headerNameOriginOin = headerNameOriginOin,
    headerNameGebruiker = headerNameGebruiker,
    headerNameToepassing = headerNameToepassing,
    toepassingValue = toepassingValue,
    systemUser = systemUser,
    logLevel = logLevel,
    apiKey = apiKey,
    headerNameApiKey = headerNameApiKey,
)
