/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.brp.util

import nl.info.zac.configuratie.BrpConfiguration
import java.util.Optional

@Suppress("LongParameterList")
fun createBrpConfiguration(
    apiKey: Optional<String> = Optional.of("apiKey"),
    originOin: Optional<String> = Optional.of("originOin"),
    auditLogProvider: Optional<String> = Optional.of("iConnect"),
    doelbindingZoekMetDefault: Optional<String> = Optional.of("queryPersonenPurpose"),
    doelbindingRaadpleegMetDefault: Optional<String> = Optional.of("retrievePersoonPurpose"),
    verwerkingregisterDefault: Optional<String> = Optional.of("processingRegisterDefault")
) = BrpConfiguration(
    apiKey = apiKey,
    originOIN = originOin,
    auditLogProvider = auditLogProvider,
    doelbindingZoekMetDefault = doelbindingZoekMetDefault,
    doelbindingRaadpleegMetDefault = doelbindingRaadpleegMetDefault,
    verwerkingregisterDefault = verwerkingregisterDefault
)
