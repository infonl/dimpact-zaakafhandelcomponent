/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.admin

import nl.info.client.zgw.ztc.model.generated.ZaakType

/**
 * Implemented by the zaaktype configuration flavours so that callers can configure a newly published
 * zaaktype without knowing which flavour backs it.
 */
interface ZaaktypeConfigurationBeheerService {
    /**
     * Creates the configuration for the given newly published [zaaktype], carrying over the data of the
     * previous version of that zaaktype, or updates it when a configuration already exists.
     */
    fun upsertConfiguration(zaaktype: ZaakType)
}
