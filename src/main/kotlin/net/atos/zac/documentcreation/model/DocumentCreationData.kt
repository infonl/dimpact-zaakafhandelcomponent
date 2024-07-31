/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.documentcreation.model

import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.model.generated.InformatieObjectType

class DocumentCreationData(
    val zaak: Zaak,
    val taskId: String? = null,
    val informatieobjecttype: InformatieObjectType
)
