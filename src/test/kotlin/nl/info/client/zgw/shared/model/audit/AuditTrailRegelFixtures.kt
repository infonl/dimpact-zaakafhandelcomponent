/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.client.zgw.shared.model.audit

import net.atos.client.zgw.shared.model.Bron
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.model.audit.AuditWijziging
import nl.info.client.zgw.zrc.model.generated.Wijzigingen
import java.net.URI
import java.time.ZonedDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun <T> createAuditTrailRegel(
    uri: URI = URI("https://example.com/resource"),
    uuid: UUID = UUID.randomUUID(),
    bron: Bron,
    applicatieId: String = UUID.randomUUID().toString(),
    applicatieWeergave: String = "ZAC",
    gebruikersId: String = "testuser",
    gebruikersWeergave: String = "Test User",
    actie: String,
    actieWeergave: String,
    resultaat: Int,
    hoofdObject: URI,
    resource: String,
    resourceUrl: URI,
    toelichting: String,
    resourceWeergave: String = "123443210 - ZAAK-2024-0000000003",
    aanmaakdatum: ZonedDateTime = ZonedDateTime.now(),
    wijzigingen: AuditWijziging<T>
) = AuditTrailRegel().apply {
    url = uri
    this.uuid = uuid
    this.bron = bron
    this.applicatieId = applicatieId
    this.applicatieWeergave = applicatieWeergave
    this.gebruikersId = gebruikersId
    this.gebruikersWeergave = gebruikersWeergave
    this.actie = actie
    this.actieWeergave = actieWeergave
    this.resultaat = resultaat
    this.hoofdObject = hoofdObject
    this.resource = resource
    this.resourceUrl = resourceUrl
    this.toelichting = toelichting
    this.resourceWeergave = resourceWeergave
    this.aanmaakdatum = aanmaakdatum
    this.wijzigingen = wijzigingen
}

@Suppress("LongParameterList")
fun createZRCAuditTrailRegel(
    uri: URI = URI("https://example.com/resource"),
    uuid: UUID = UUID.randomUUID(),
    bron: Bron,
    applicatieId: String = UUID.randomUUID().toString(),
    applicatieWeergave: String = "ZAC",
    gebruikersId: String = "testuser",
    gebruikersWeergave: String = "Test User",
    actie: String,
    actieWeergave: String,
    resultaat: Int,
    hoofdObject: URI,
    resource: String,
    resourceUrl: URI,
    toelichting: String,
    resourceWeergave: String = "123443210 - ZAAK-2024-0000000003",
    aanmaakdatum: ZonedDateTime = ZonedDateTime.now(),
    wijzigingen: Wijzigingen
) = ZRCAuditTrailRegel(
    uri,
    uuid,
    bron,
    applicatieId,
    applicatieWeergave,
    gebruikersId,
    gebruikersWeergave,
    actie,
    actieWeergave,
    resultaat,
    hoofdObject,
    resource,
    resourceUrl,
    toelichting,
    resourceWeergave,
    aanmaakdatum,
    wijzigingen
)
