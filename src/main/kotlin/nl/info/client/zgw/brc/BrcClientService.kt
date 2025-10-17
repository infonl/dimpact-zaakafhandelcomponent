/*
 * SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.brc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import nl.info.client.zgw.brc.model.BesluitenListParameters
import nl.info.client.zgw.brc.model.generated.Besluit
import nl.info.client.zgw.brc.model.generated.BesluitInformatieObject
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URI
import java.util.UUID

/**
 * Besluiten client service.
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrcClientService @Inject constructor(
    @RestClient
    private val brcClient: BrcClient,
    private val zgwClientHeadersFactory: ZGWClientHeadersFactory
) {
    fun listBesluiten(zaak: Zaak): List<Besluit> = BesluitenListParameters()
        .apply { this.zaakUri = zaak.url }
        .let { brcClient.besluitList(it).results }

    fun createBesluit(besluit: Besluit): Besluit = brcClient.besluitCreate(besluit)

    fun updateBesluit(besluit: Besluit, toelichting: String?): Besluit {
        toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return brcClient.besluitUpdate(besluit.url.extractUuid(), besluit)
    }

    fun listAuditTrail(besluitUuid: UUID): List<AuditTrailRegel> = brcClient.listAuditTrail(besluitUuid)

    fun readBesluit(uuid: UUID): Besluit = brcClient.besluitRead(uuid)

    fun createBesluitInformatieobject(
        besluitInformatieobject: BesluitInformatieObject,
        toelichting: String?
    ): BesluitInformatieObject {
        toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return brcClient.besluitinformatieobjectCreate(besluitInformatieobject)
    }

    fun deleteBesluitinformatieobject(besluitInformatieobjectUuid: UUID): BesluitInformatieObject =
        brcClient.besluitinformatieobjectDelete(besluitInformatieobjectUuid)

    fun listBesluitInformatieobjecten(besluit: URI): List<BesluitInformatieObject> =
        brcClient.listBesluitInformatieobjectenByBesluit(besluit)

    fun isInformatieObjectGekoppeldAanBesluit(informatieobject: URI) =
        brcClient.listBesluitInformatieobjectenByInformatieObject(
            informatieobject
        ).isNotEmpty()
}
