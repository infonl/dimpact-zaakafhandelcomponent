/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp.util

import nl.info.zac.configuratie.BrpConfiguration
import java.util.Optional

@Suppress("LongParameterList")
fun createBrpConfiguration(
    apiKey: Optional<String> = Optional.of("fakeApiKey"),
    originOin: Optional<String> = Optional.of("fakeOriginOin"),
    brpProtocolleringProvider: Optional<String> = Optional.of("fakeBrpProtocolleringProvider"),
    doelbindingZoekMetDefault: Optional<String> = Optional.of("fakeDoelbindingZoekMetDefault"),
    doelbindingRaadpleegMetDefault: Optional<String> = Optional.of("fakeDoelbindingRaadpleegMetDefault"),
    verwerkingregisterDefault: Optional<String> = Optional.of("fakeVerwerkingregisterDefault")
) = BrpConfiguration(
    apiKey = apiKey,
    originOIN = originOin,
    brpProtocolleringProvider = brpProtocolleringProvider,
    doelbindingZoekMetDefault = doelbindingZoekMetDefault,
    doelbindingRaadpleegMetDefault = doelbindingRaadpleegMetDefault,
    verwerkingsregister = verwerkingregisterDefault
)
