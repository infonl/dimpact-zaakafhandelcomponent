/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType

data class DocumentCreationData(
    val zaak: Zaak,

    val taskId: String? = null,

    @Deprecated(
        "Only required for the SmartDocuments unattended flow. " +
            "Will be removed in future once we no longer support the SmartDocuments attended flow."
    )
    val informatieobjecttype: InformatieObjectType? = null,

    /**
     * Nullable for now but once we no longer support the SmartDocuments attended flow we should make this required
     */
    val templateGroup: String? = null,

    /**
     * Nullable for now but once we no longer support the SmartDocuments attended flow we should make this required
     */
    val template: String? = null
)
