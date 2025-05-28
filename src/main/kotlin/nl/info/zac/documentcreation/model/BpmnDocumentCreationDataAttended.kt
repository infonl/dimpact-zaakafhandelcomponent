/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.documentcreation.model

import java.time.ZonedDateTime
import java.util.UUID
import nl.info.client.zgw.zrc.model.generated.Zaak

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
