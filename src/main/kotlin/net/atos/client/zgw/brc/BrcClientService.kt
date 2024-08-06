/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.zgw.brc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import net.atos.client.zgw.brc.model.BesluitenListParameters
import net.atos.client.zgw.brc.model.generated.Besluit
import net.atos.client.zgw.brc.model.generated.BesluitInformatieObject
import net.atos.client.zgw.shared.model.audit.AuditTrailRegel
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.zac.util.UriUtil
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URI
import java.util.Optional
import java.util.UUID

/**
 * BRC Client Service
 */
@ApplicationScoped
@AllOpen
@NoArgConstructor
class BrcClientService @Inject constructor(
    @RestClient
    private val brcClient: BrcClient,
    private val zgwClientHeadersFactory: ZGWClientHeadersFactory
) {
    fun listBesluiten(zaak: Zaak): Optional<List<Besluit>> {
        val listParameters = BesluitenListParameters().apply { this.zaakUri = zaak.url }
        val results = brcClient.besluitList(listParameters)
        return if (results.count > 0) {
            Optional.of(results.results)
        } else {
            Optional.empty()
        }
    }

    fun createBesluit(besluit: Besluit): Besluit = brcClient.besluitCreate(besluit)

    fun updateBesluit(besluit: Besluit, toelichting: String?): Besluit {
        toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return brcClient.besluitUpdate(UriUtil.uuidFromURI(besluit.url), besluit)
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
