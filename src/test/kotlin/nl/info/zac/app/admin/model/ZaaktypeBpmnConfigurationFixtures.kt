/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.admin.model

import net.atos.zac.app.admin.model.RestZaakbeeindigParameter
import nl.info.zac.app.zaak.model.RestResultaattype
import java.util.UUID

fun createRestZaaktypeBpmnConfiguration(
    id: Long = 1L,
    zaaktypeUuid: UUID = UUID.randomUUID(),
    bpmnProcessDefinitionKey: String = "fakeProcessDefinitionKey",
    zaaktypeOmschrijving: String = "fakeZaaktypeOmschrijving",
    groepNaam: String? = "fakeGroupName",
    productaanvraagtype: String? = null,
    zaakNietOntvankelijkResultaattype: RestResultaattype? = null,
    zaakBeeindigParameters: List<RestZaakbeeindigParameter> = emptyList()
) = RestZaaktypeBpmnConfiguration(
    id = id,
    zaaktypeUuid = zaaktypeUuid,
    bpmnProcessDefinitionKey = bpmnProcessDefinitionKey,
    zaaktypeOmschrijving = zaaktypeOmschrijving,
    groepNaam = groepNaam,
    productaanvraagtype = productaanvraagtype,
    zaakNietOntvankelijkResultaattype = zaakNietOntvankelijkResultaattype,
    zaakbeeindigParameters = zaakBeeindigParameters
)
