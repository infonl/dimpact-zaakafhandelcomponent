/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.configuration

import java.util.logging.Level

interface BrpConfigurationValue {
    fun isAvailable(): Boolean
    fun getHeaderName(): String
    fun getValue(): String?
}

@Suppress("TooManyFunctions")
interface BrpConfigurationProvider {
    fun isBrpProtocolleringEnabled(): Boolean
    fun isDoelbindingPerZaaktypeEnabled(): Boolean
    fun getLogLevel(): Level
    fun getHeaderUser(): String?
    fun getOriginOIN(): BrpConfigurationValue
    fun getDoelbindingZoekMetDefault(): BrpConfigurationValue
    fun getDoelbindingRaadpleegMetDefault(): BrpConfigurationValue
    fun getVerwerkingRegisterDefault(): BrpConfigurationValue
    fun getToepassing(): BrpConfigurationValue
    fun getApiKey(): BrpConfigurationValue
    fun buildDoelbinding(doelbindingSupplier: () -> String?): BrpConfigurationValue
    fun buildVerwerkingRegister(verwerkingSupplier: () -> String?): BrpConfigurationValue
    fun buildUser(userSupplier: () -> String?): BrpConfigurationValue
}
