/*
 * SPDX-FileCopyrightText: 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin.model

import jakarta.persistence.DiscriminatorValue

@DiscriminatorValue("UNKNOWN")
class ZaaktypeUnknownConfiguration : ZaaktypeConfiguration() {
    override fun getConfigurationType() = Companion.ZaaktypeConfigurationType.UNKNOWN
}
