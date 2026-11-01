/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.app.detacheddocuments

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import nl.info.client.zgw.drc.DrcClientService
import nl.info.client.zgw.drc.model.generated.EnkelvoudigInformatieObject
import nl.info.client.zgw.shared.exception.ZgwErrorException
import nl.info.client.zgw.shared.exception.ZgwValidationErrorException
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.model.generated.Zaak
import nl.info.zac.app.detacheddocuments.converter.RestDetachedDocumentConverter
import nl.info.zac.app.detacheddocuments.converter.toDetachedDocumentListParameters
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocument
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFailure
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkFoutcode
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkLinkRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkRequest
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentBulkResult
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentListParameters
import nl.info.zac.app.detacheddocuments.model.RestDetachedDocumentResult
import nl.info.zac.app.identity.converter.RestUserConverter
import nl.info.zac.app.informatieobjecten.model.RestDocumentVerplaatsGegevens
import nl.info.zac.app.shared.RestResultaat
import nl.info.zac.authentication.LoggedInUser
import nl.info.zac.document.detacheddocument.DetachedDocumentLockedException
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.document.detacheddocument.repository.model.DetachedDocument
import nl.info.zac.policy.PolicyService
import nl.info.zac.policy.assertPolicy
import nl.info.zac.policy.exception.PolicyException
import nl.info.zac.search.SearchService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.jetty.http.HttpStatus
import java.util.logging.Logger

@Singleton
@Path("ontkoppeldedocumenten")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@AllOpen
@NoArgConstructor
class DetachedDocumentRestService @Inject constructor(
    private val detachedDocumentService: DetachedDocumentService,
    private val drcClientService: DrcClientService,
    private val zrcClientService: ZrcClientService,
    private val restDetachedDocumentConverter: RestDetachedDocumentConverter,
    private val userConverter: RestUserConverter,
    private val policyService: PolicyService,
    private val searchService: SearchService,
    private val loggedInUserInstance: Instance<LoggedInUser>
) {
    companion object {
        private val LOG = Logger.getLogger(DetachedDocumentRestService::class.java.name)
    }

    @PUT
    @Path("")
    fun listDetachedDocuments(
        restListParameters: RestDetachedDocumentListParameters
    ): RestResultaat<RestDetachedDocument> {
        assertPolicy(policyService.readWerklijstRechten().canInbox)
        val listParameters = restListParameters.toDetachedDocumentListParameters()
        val resultaat = detachedDocumentService.getDetachedDocumentResult(listParameters)
        val ontkoppeldeDocumenten = resultaat.items
        val informationObjectTypeUUIDs = ontkoppeldeDocumenten.map { ontkoppeldeDocument ->
            drcClientService
                .readEnkelvoudigInformatieobject(ontkoppeldeDocument.documentUUID)
                .informatieobjecttype
                .extractUuid()
        }
        val restDetachedDocumentResult = RestDetachedDocumentResult(
            results = restDetachedDocumentConverter.convertToRestDetachedDocuments(
                documenten = ontkoppeldeDocumenten,
                informatieobjectTypeUUIDs = informationObjectTypeUUIDs,
                zaakspecifiekGeautoriseerdeZaakIDs = searchService.listZaakspecifiekGeautoriseerdeWaarden(
                    ontkoppeldeDocumenten.map { it.zaakID }
                )
            ),
            resultCount = resultaat.count
        )
        val ontkoppeldDoor = resultaat.detachedByFilter
        if (ontkoppeldDoor.isEmpty()) {
            restListParameters.ontkoppeldDoor?.let { user ->
                restDetachedDocumentResult.filterOntkoppeldDoor = listOf(user)
            }
        } else {
            restDetachedDocumentResult.filterOntkoppeldDoor = with(userConverter) {
                ontkoppeldDoor.convertUsernames(logContext = "the ontkoppeld door filter of the ontkoppelde documenten")
            }
        }
        return restDetachedDocumentResult
    }

    @DELETE
    @Path("{id}")
    fun deleteDetachedDocument(@PathParam("id") id: Long) {
        assertPolicy(policyService.readWerklijstRechten().canOntkoppeldeDocumentenVerwijderen)
        detachedDocumentService.find(id)?.let { removeDetachedDocument(it, rejectLocked = false) }
    }

    @PUT
    @Path("lijst/verwijderen")
    fun deleteDetachedDocuments(@Valid request: RestDetachedDocumentBulkRequest): RestDetachedDocumentBulkResult {
        assertPolicy(policyService.readWerklijstRechten().canOntkoppeldeDocumentenVerwijderen)
        return processInBulk(request.ids) { removeDetachedDocument(it, rejectLocked = true) }
    }

    @PUT
    @Path("lijst/koppelen")
    fun linkDetachedDocuments(@Valid request: RestDetachedDocumentBulkLinkRequest): RestDetachedDocumentBulkResult {
        assertPolicy(policyService.readWerklijstRechten().canInbox)
        val targetZaak = zrcClientService.readZaakByID(request.nieuweZaakID)
        return processInBulk(request.ids) { linkDetachedDocument(it, targetZaak) }
    }

    private fun processInBulk(
        ids: List<Long>,
        process: (DetachedDocument) -> Unit
    ): RestDetachedDocumentBulkResult {
        val processed = mutableListOf<Long>()
        val failed = mutableListOf<RestDetachedDocumentBulkFailure>()
        ids.distinct().forEach { id ->
            val detachedDocument = detachedDocumentService.find(id)
            if (detachedDocument == null) {
                failed.add(RestDetachedDocumentBulkFailure(id, null, RestDetachedDocumentBulkFoutcode.NIET_GEVONDEN))
                return@forEach
            }
            try {
                process(detachedDocument)
                processed.add(id)
            } catch (detachedDocumentLockedException: DetachedDocumentLockedException) {
                LOG.info(detachedDocumentLockedException.message)
                failed.add(failure(id, detachedDocument, RestDetachedDocumentBulkFoutcode.VERGRENDELD))
            } catch (illegalStateException: IllegalStateException) {
                LOG.info(illegalStateException.message)
                failed.add(failure(id, detachedDocument, RestDetachedDocumentBulkFoutcode.REEDS_GEKOPPELD))
            } catch (policyException: PolicyException) {
                LOG.info("Geen recht op ontkoppeld document '$id': ${policyException.message}")
                failed.add(failure(id, detachedDocument, RestDetachedDocumentBulkFoutcode.GEEN_RECHT))
            } catch (zgwErrorException: ZgwErrorException) {
                LOG.warning("Ontkoppeld document '$id' kon niet worden verwerkt: ${zgwErrorException.message}")
                failed.add(failure(id, detachedDocument, RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT))
            } catch (zgwValidationErrorException: ZgwValidationErrorException) {
                LOG.warning(
                    "Ontkoppeld document '$id' kon niet worden verwerkt: ${zgwValidationErrorException.message}"
                )
                failed.add(failure(id, detachedDocument, RestDetachedDocumentBulkFoutcode.OPEN_ZAAK_FOUT))
            }
        }
        return RestDetachedDocumentBulkResult(verwerkt = processed, mislukt = failed)
    }

    private fun failure(id: Long, detachedDocument: DetachedDocument, foutcode: RestDetachedDocumentBulkFoutcode) =
        RestDetachedDocumentBulkFailure(id, detachedDocument.titel, foutcode)

    private fun removeDetachedDocument(detachedDocument: DetachedDocument, rejectLocked: Boolean) {
        var enkelvoudigInformatieobject: EnkelvoudigInformatieObject? = null
        val documentUUID = detachedDocument.documentUUID
        try {
            enkelvoudigInformatieobject = drcClientService.readEnkelvoudigInformatieobject(documentUUID)
        } catch (zgwErrorException: ZgwErrorException) {
            if (zgwErrorException.zgwError.status != HttpStatus.NOT_FOUND_404) {
                throw zgwErrorException
            }
            LOG.info("Document met UUID '$documentUUID' wel gevonden in de database, maar niet in OpenZaak")
        }
        enkelvoudigInformatieobject?.let { informatieobject ->
            if (rejectLocked && informatieobject.getLocked()) {
                throw DetachedDocumentLockedException("Informatieobject '$documentUUID' is vergrendeld")
            }
            val zaakInformatieobjecten = zrcClientService.listZaakinformatieobjecten(informatieobject)
            if (zaakInformatieobjecten.isNotEmpty()) {
                val zaakUuid = zaakInformatieobjecten.first().zaak.extractUuid()
                error("Informatieobject is gekoppeld aan zaak '$zaakUuid'")
            }
            drcClientService.deleteEnkelvoudigInformatieobject(documentUUID)
        }
        detachedDocument.id?.let(detachedDocumentService::deleteIfExists)
    }

    private fun linkDetachedDocument(detachedDocument: DetachedDocument, targetZaak: Zaak) {
        val informatieobject = drcClientService.readEnkelvoudigInformatieobject(detachedDocument.documentUUID)
        assertPolicy(
            policyService.readDocumentRechten(informatieobject, targetZaak).canVerplaatsen &&
                policyService.readZaakRechten(targetZaak, loggedInUserInstance.get()).canWijzigen
        )
        zrcClientService.koppelInformatieobject(
            informatieobject,
            targetZaak,
            "Verplaatst: ${RestDocumentVerplaatsGegevens.ONTKOPPELDE_DOCUMENTEN} -> ${targetZaak.identificatie}"
        )
        detachedDocumentService.deleteIfExists(detachedDocument.documentUUID)
    }
}
