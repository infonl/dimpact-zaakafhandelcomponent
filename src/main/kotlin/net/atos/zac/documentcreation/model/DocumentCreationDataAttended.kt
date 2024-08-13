/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType

@Deprecated(
    "Only required for the SmartDocuments attended flow. " +
        "Will be removed in future once we no longer support the SmartDocuments attended flow."
)
data class DocumentCreationDataAttended(
    val informatieobjecttype: InformatieObjectType? = null,

    val taskId: String? = null,

    val zaak: Zaak
)
