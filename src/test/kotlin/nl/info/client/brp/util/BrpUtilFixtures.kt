/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp.util

import nl.info.zac.configuration.BrpConfiguration
import java.util.Optional

@Suppress("LongParameterList")
fun createBrpConfiguration(
    originOin: Optional<String> = Optional.of("fakeOriginOin"),
    brpProtocolleringProvider: Optional<String> = Optional.of("iConnect"),
    doelbindingZoekMetDefault: Optional<String> = Optional.of("fakeDoelbindingZoekMetDefault"),
    doelbindingRaadpleegMetDefault: Optional<String> = Optional.of("fakeDoelbindingRaadpleegMetDefault"),
    verwerkingregisterDefault: Optional<String> = Optional.of("fakeVerwerkingregisterDefault"),
    headerNameDoelbinding: Optional<String> = Optional.of("x-doelbinding"),
    headerNameVerwerking: Optional<String> = Optional.of("x-verwerking"),
    headerNameOriginOin: Optional<String> = Optional.of("x-origin-oin"),
    headerNameGebruiker: Optional<String> = Optional.of("x-gebruiker"),
    headerNameToepassing: Optional<String> = Optional.of("x-toepassing"),
    toepassingValue: Optional<String> = Optional.of("ZAC"),
) = BrpConfiguration(
    originOIN = originOin,
    brpProtocolleringProvider = brpProtocolleringProvider,
    doelbindingZoekMetDefault = doelbindingZoekMetDefault,
    doelbindingRaadpleegMetDefault = doelbindingRaadpleegMetDefault,
    verwerkingsregister = verwerkingregisterDefault,
    headerNameDoelbinding = headerNameDoelbinding,
    headerNameVerwerking = headerNameVerwerking,
    headerNameOriginOin = headerNameOriginOin,
    headerNameGebruiker = headerNameGebruiker,
    headerNameToepassing = headerNameToepassing,
    toepassingValue = toepassingValue,
)
