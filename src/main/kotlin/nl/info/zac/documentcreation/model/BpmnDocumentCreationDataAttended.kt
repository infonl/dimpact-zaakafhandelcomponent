/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation.model

import net.atos.client.zgw.zrc.model.Zaak
import java.time.ZonedDateTime
import java.util.UUID

data class BpmnDocumentCreationDataAttended(
    val zaak: Zaak,

    val taskId: String?,

    val templateGroupName: String,

    val templateName: String,

    val informatieobjecttypeUuid: UUID,

    var title: String,

    var description: String? = null,

    var author: String? = null,

    var creationDate: ZonedDateTime,
)
