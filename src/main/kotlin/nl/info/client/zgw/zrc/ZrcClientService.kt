/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.NotFoundException
import net.atos.client.zgw.shared.model.Results
import net.atos.client.zgw.shared.util.ZGWClientHeadersFactory
import net.atos.client.zgw.zrc.model.BetrokkeneType
import net.atos.client.zgw.zrc.model.Rol
import net.atos.client.zgw.zrc.model.RolListParameters
import net.atos.client.zgw.zrc.model.Status
import net.atos.client.zgw.zrc.model.ZaakInformatieobject
import net.atos.client.zgw.zrc.model.ZaakInformatieobjectListParameters
import net.atos.client.zgw.zrc.model.ZaakListParameters
import net.atos.client.zgw.zrc.model.ZaakUuid
import net.atos.client.zgw.zrc.model.zaakobjecten.Zaakobject
import net.atos.client.zgw.zrc.model.zaakobjecten.ZaakobjectListParameters
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.shared.model.audit.ZRCAuditTrailRegel
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.util.validateZgwApiUri
import nl.info.client.zgw.zrc.model.generated.Resultaat
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.client.zgw.zrc.util.isOpen
import nl.info.zac.configuratie.ConfiguratieService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.rest.client.inject.RestClient
import java.net.URI
import java.util.UUID

@ApplicationScoped
@NoArgConstructor
@AllOpen
@Suppress("TooManyFunctions")
class ZrcClientService @Inject constructor(
    @RestClient
    private val zrcClient: ZrcClient,

    private val zgwClientHeadersFactory: ZGWClientHeadersFactory,

    private val configuratieService: ConfiguratieService
) {
    fun createRol(rol: Rol<*>) = createRol(rol, null)

    fun createRol(rol: Rol<*>, toelichting: String?): Rol<*> {
        toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return zrcClient.rolCreate(rol)
    }

    fun deleteRol(rol: Rol<*>, toelichting: String?) {
        toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        zrcClient.rolDelete(rol.uuid)
    }

    fun createZaakobject(zaakobject: Zaakobject): Zaakobject =
        zrcClient.zaakobjectCreate(zaakobject)

    fun deleteZaakobject(zaakobject: Zaakobject, toelichting: String?) {
        toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        zrcClient.zaakobjectDelete(zaakobject.uuid)
    }

    fun readZaakobject(zaakobjectUUID: UUID): Zaakobject = zrcClient.zaakobjectRead(zaakobjectUUID)

    fun createZaakInformatieobject(
        zaakInformatieobject: ZaakInformatieobject,
        toelichting: String?
    ): ZaakInformatieobject {
        toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return zrcClient.zaakinformatieobjectCreate(zaakInformatieobject)
    }

    fun deleteZaakInformatieobject(
        zaakInformatieobjectUuid: UUID,
        toelichting: String?,
        toelichtingPrefix: String?
    ) {
        val fullToelichting = toelichting?.let { "$toelichtingPrefix: $it" } ?: toelichtingPrefix
        zgwClientHeadersFactory.setAuditToelichting(fullToelichting)
        zrcClient.zaakinformatieobjectDelete(zaakInformatieobjectUuid)
    }

    fun readZaak(zaakUUID: UUID): Zaak = zrcClient.zaakRead(zaakUUID)

    fun readZaak(zaakURI: URI): Zaak {
        validateZgwApiUri(zaakURI, configuratieService.readZgwApiClientMpRestUrl())
        return readZaak(zaakURI.extractUuid())
    }

    fun readZaakinformatieobject(zaakinformatieobjectUUID: UUID): ZaakInformatieobject =
        zrcClient.zaakinformatieobjectRead(zaakinformatieobjectUUID)

    fun updateRol(zaak: Zaak, rol: Rol<*>, toelichting: String?) {
        val rollen = listRollen(zaak).toMutableList().apply { add(rol) }
        updateRollen(zaak, rollen, toelichting)
    }

    fun deleteRol(zaak: Zaak, betrokkeneType: BetrokkeneType?, toelichting: String?) {
        val rollen = listRollen(zaak).toMutableList().apply {
            firstOrNull { it.betrokkeneType == betrokkeneType }?.let { betrokkene ->
                removeAll { it.equalBetrokkeneRol(betrokkene) }
            }
        }
        updateRollen(zaak, rollen, toelichting)
    }

    fun readRol(rolURI: URI): Rol<*> {
        validateZgwApiUri(rolURI, configuratieService.readZgwApiClientMpRestUrl())
        return readRol(rolURI.extractUuid())
    }

    fun readRol(rolUUID: UUID): Rol<*> = zrcClient.rolRead(rolUUID)

    fun readResultaat(resultaatURI: URI): Resultaat {
        validateZgwApiUri(resultaatURI, configuratieService.readZgwApiClientMpRestUrl())
        return zrcClient.resultaatRead(resultaatURI.extractUuid())
    }

    fun readStatus(statusURI: URI): Status {
        validateZgwApiUri(statusURI, configuratieService.readZgwApiClientMpRestUrl())
        return zrcClient.statusRead(statusURI.extractUuid())
    }

    fun listZaakobjecten(zaakobjectListParameters: ZaakobjectListParameters): Results<Zaakobject> =
        zrcClient.zaakobjectList(zaakobjectListParameters)

    fun patchZaak(zaakUUID: UUID, zaak: Zaak, explanation: String?): Zaak {
        explanation?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return patchZaak(zaakUUID, zaak)
    }

    fun patchZaak(zaakUUID: UUID, zaak: Zaak): Zaak = zrcClient.zaakPartialUpdate(zaakUUID, zaak)

    fun listZaken(filter: ZaakListParameters): Results<Zaak> = zrcClient.zaakList(filter)

    fun listZakenUuids(filter: ZaakListParameters): Results<ZaakUuid> = zrcClient.zaakListUuids(filter)

    fun listZaakinformatieobjecten(filter: ZaakInformatieobjectListParameters): List<ZaakInformatieobject> =
        zrcClient.zaakinformatieobjectList(filter)

    fun listZaakinformatieobjecten(zaak: Zaak): List<ZaakInformatieobject> =
        listZaakinformatieobjecten(
            ZaakInformatieobjectListParameters().apply {
                this.zaak = zaak.url
            }
        )

    fun listZaakinformatieobjecten(informatieobject: EnkelvoudigInformatieObject): List<ZaakInformatieobject> =
        listZaakinformatieobjecten(
            ZaakInformatieobjectListParameters().apply {
                this.informatieobject = informatieobject.getUrl()
            }
        )

    fun listRollen(filter: RolListParameters): Results<Rol<*>> = zrcClient.rolList(filter)

    fun listRollen(zaak: Zaak): List<Rol<*>> =
        zrcClient.rolList(RolListParameters(zaak.url)).getResults()

    fun readZaakByID(identificatie: String): Zaak {
        val zaakResults = listZaken(ZaakListParameters().apply { this.identificatie = identificatie })
        require(zaakResults.count <= 1) {
            "Meerdere zaken met identificatie '$identificatie' gevonden"
        }
        return zaakResults.getResults().firstOrNull()
            ?: throw NotFoundException("Zaak met identificatie '$identificatie' niet gevonden")
    }

    fun verplaatsInformatieobject(
        informatieobject: EnkelvoudigInformatieObject,
        oudeZaak: Zaak,
        nieuweZaak: Zaak
    ) {
        val parameters = ZaakInformatieobjectListParameters().apply {
            this.informatieobject = informatieobject.getUrl()
            this.zaak = oudeZaak.url
        }
        val zaakInformatieobjecten = listZaakinformatieobjecten(parameters)
        require(zaakInformatieobjecten.isNotEmpty()) {
            "Geen ZaakInformatieobject gevonden voor Zaak: '${oudeZaak.identificatie}' " +
                "en InformatieObject: '${informatieobject.getInhoud().extractUuid()}'"
        }
        val oudeZaakInformatieobject = zaakInformatieobjecten.first()
        val nieuweZaakInformatieObject = ZaakInformatieobject().apply {
            this.zaak = nieuweZaak.url
            this.informatieobject = informatieobject.getUrl()
            this.titel = oudeZaakInformatieobject.titel
            this.beschrijving = oudeZaakInformatieobject.beschrijving
        }

        val toelichting = "${oudeZaak.identificatie} -> ${nieuweZaak.identificatie}"
        createZaakInformatieobject(nieuweZaakInformatieObject, toelichting)
        deleteZaakInformatieobject(oudeZaakInformatieobject.uuid, toelichting, "Verplaatst")
    }

    fun koppelInformatieobject(
        enkelvoudigInformatieObject: EnkelvoudigInformatieObject,
        targetZaak: Zaak,
        description: String?
    ) {
        listZaakinformatieobjecten(enkelvoudigInformatieObject).firstOrNull()?.let {
            throw IllegalStateException("Informatieobject is reeds gekoppeld aan zaak '${it.zaak.extractUuid()}'")
        }
        val nieuweZaakInformatieObject = ZaakInformatieobject().apply {
            zaak = targetZaak.url
            informatieobject = enkelvoudigInformatieObject.getUrl()
            titel = enkelvoudigInformatieObject.getTitel()
            beschrijving = enkelvoudigInformatieObject.getBeschrijving()
        }
        createZaakInformatieobject(nieuweZaakInformatieObject, description)
    }

    fun listAuditTrail(zaakUUID: UUID): List<ZRCAuditTrailRegel> =
        zrcClient.listAuditTrail(zaakUUID)

    fun createResultaat(resultaat: Resultaat): Resultaat? {
        resultaat.toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return zrcClient.resultaatCreate(resultaat)
    }

    fun updateResultaat(resultaat: Resultaat): Resultaat? {
        resultaat.toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return zrcClient.resultaatUpdate(resultaat.getUuid(), resultaat)
    }

    fun deleteResultaat(resultaatUUID: UUID) = zrcClient.resultaatDelete(resultaatUUID)

    fun createZaak(zaak: Zaak): Zaak {
        zaak.toelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return zrcClient.zaakCreate(zaak)
    }

    fun createStatus(status: Status): Status {
        status.statustoelichting?.let { zgwClientHeadersFactory.setAuditToelichting(it) }
        return zrcClient.statusCreate(status)
    }

    fun heeftOpenDeelzaken(zaak: Zaak): Boolean =
        zaak.deelzaken.map { zaakURI -> this.readZaak(zaakURI) }.any { it.isOpen() }

    private fun deleteDeletedRollen(
        currentRoles: List<Rol<*>>,
        rolesToBeDeleted: List<Rol<*>>,
        description: String?
    ) {
        currentRoles
            .filter { currentRole -> rolesToBeDeleted.none { it.equalBetrokkeneRol(currentRole) } }
            .forEach { deleteRol(it, description) }
    }

    /**
     * Updates the [Rol]s for a [Zaak].
     * Replaces all existing [Rol]s with the provided roles.
     *
     * @param zaak the zaak
     * @param rollen the roles to be updated
     */
    private fun updateRollen(zaak: Zaak, rollen: List<Rol<*>>, toelichting: String?) {
        val current = listRollen(zaak)
        deleteDeletedRollen(current, rollen, toelichting)
        deleteUpdatedRollen(current, rollen, toelichting)
        createUpdatedRollen(current, rollen, toelichting)
        createCreatedRollen(current, rollen, toelichting)
    }

    private fun deleteUpdatedRollen(
        currentRoles: List<Rol<*>>,
        rolesToBeDeleted: List<Rol<*>>,
        description: String?
    ) = currentRoles
        .filter { oud -> rolesToBeDeleted.any { it.equalBetrokkeneRol(oud) && it != oud } }
        .forEach { deleteRol(it, description) }

    private fun createUpdatedRollen(
        currentRoles: List<Rol<*>>,
        rolesToBeUpdated: List<Rol<*>>,
        description: String?
    ) = rolesToBeUpdated
        .filter { newRole -> currentRoles.any { it.equalBetrokkeneRol(newRole) && it != newRole } }
        .forEach { createRol(it, description) }

    private fun createCreatedRollen(
        currentRoles: List<Rol<*>>,
        rolesToBeCreated: List<Rol<*>>,
        description: String?
    ) = rolesToBeCreated
        .filter { newRole -> currentRoles.none { it.equalBetrokkeneRol(newRole) } }
        .forEach { createRol(it, description) }
}
