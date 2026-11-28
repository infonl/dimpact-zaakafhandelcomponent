/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import nl.info.client.zgw.util.extractUuid
import nl.info.client.zgw.zrc.ZrcClientService
import nl.info.client.zgw.zrc.util.ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
import nl.info.client.zgw.zrc.util.isZaakspecifiekGeautoriseerd
import nl.info.client.zgw.ztc.ZtcClientService
import nl.info.zac.admin.ZaaktypeConfigurationBeheerService
import nl.info.zac.authentication.ActiveSession
import nl.info.zac.authentication.setFunctioneelGebruiker
import nl.info.zac.document.detacheddocument.DetachedDocumentService
import nl.info.zac.document.inboxdocument.InboxDocumentService
import nl.info.zac.event.EventingService
import nl.info.zac.flowable.ZaakProcessService
import nl.info.zac.log.ZaakEvent
import nl.info.zac.log.ZacLogger
import nl.info.zac.notification.log.FormerBehandelaarSignaleringenDeleted
import nl.info.zac.notification.log.NotificationHandlingFailed
import nl.info.zac.notification.log.NotificationReceived
import nl.info.zac.notification.log.TestNotificationReceived
import nl.info.zac.notification.log.ZaakSignaleringenDeleted
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.search.IndexingService
import nl.info.zac.signalering.SignaleringService
import nl.info.zac.signalering.event.getSignaleringEvents
import nl.info.zac.signalering.model.SignaleringSubject
import nl.info.zac.signalering.model.SignaleringVerzondenZoekParameters
import nl.info.zac.signalering.model.SignaleringZoekParameters
import nl.info.zac.task.TaskService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import nl.info.zac.websocket.event.FixedScreenEventResourceId
import nl.info.zac.websocket.event.ScreenEventType
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.UUID

/**
 * Provides REST endpoints for receiving notifications about events that ZAC needs to know about
 * so that it can take appropriate action.
 */
@Path("notificaties")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Singleton
@NoArgConstructor
@AllOpen
@Suppress("LongParameterList", "TooManyFunctions")
class NotificationReceiver @Inject constructor(
    private val eventingService: EventingService,
    private val productaanvraagService: ProductaanvraagService,
    private val indexingService: IndexingService,
    private val inboxDocumentService: InboxDocumentService,
    private val detachedDocumentService: DetachedDocumentService,
    private val zaaktypeConfigurationBeheerService: ZaaktypeConfigurationBeheerService,
    private val zaakProcessService: ZaakProcessService,
    private val signaleringService: SignaleringService,
    private val taskService: TaskService,
    private val zrcClientService: ZrcClientService,
    private val ztcClientService: ZtcClientService,

    @ConfigProperty(name = "OPEN_NOTIFICATIONS_API_SECRET_KEY")
    private val secret: String,

    @ActiveSession
    private val httpSession: Instance<HttpSession>
) {
    companion object {
        private val LOG = ZacLogger.getLogger(NotificationReceiver::class)
        private const val OBJECTTYPE_KENMERK = "objectType"
    }

    @POST
    fun notificatieReceive(@Context headers: HttpHeaders, notification: Notification): Response {
        if (!isAuthenticated(headers)) {
            return Response.noContent().status(Response.Status.FORBIDDEN).build()
        }
        setFunctioneelGebruiker(httpSession.get())
        LOG.info(notification.toNotificationReceived())
        handleSignaleringen(notification)
        handleProductaanvraag(notification)
        handleIndexing(notification)
        handleInboxDocuments(notification)
        handleDetachedDocuments(notification)
        handleFlowableProcessData(notification)
        handleZaaktype(notification)
        handleBesluittypeAndInformatieobjecttype(notification)
        handleWebsockets(notification)
        return Response.noContent().build()
    }

    /**
     * Deletes zaak and related task signaleringen as well as 'zaak sent (verzonden)' and 'task sent (verzonden)' records
     * for the given zaak UUID.
     */
    private fun deleteSignaleringenForZaak(zaakUUID: UUID) {
        val zaakSignaleringCount = signaleringService.deleteSignaleringen(
            SignaleringZoekParameters(SignaleringSubject.ZAAK, zaakUUID.toString())
        )
        val isZaakSignaleringVerzondenDeleted = signaleringService.deleteSignaleringVerzonden(
            SignaleringVerzondenZoekParameters(SignaleringSubject.ZAAK, zaakUUID.toString())
        )
        var taakSignaleringCount = 0
        var taakSignaleringVerzondenCount = 0
        taskService.listTasksForZaak(zaakUUID).forEach { task ->
            taakSignaleringCount += signaleringService.deleteSignaleringen(
                SignaleringZoekParameters(SignaleringSubject.TAAK, task.id)
            )
            if (signaleringService.deleteSignaleringVerzonden(
                    SignaleringVerzondenZoekParameters(SignaleringSubject.TAAK, task.id)
                )
            ) {
                taakSignaleringVerzondenCount++
            }
        }
        LOG.info(
            ZaakSignaleringenDeleted(
                zaakUuid = zaakUUID,
                zaakSignaleringCount = zaakSignaleringCount,
                isZaakSignaleringVerzondenDeleted = isZaakSignaleringVerzondenDeleted,
                taakSignaleringCount = taakSignaleringCount,
                taakSignaleringVerzondenCount = taakSignaleringVerzondenCount
            )
        )
    }

    private fun deleteZaakSignaleringenOfFormerBehandelaren(zaakUUID: UUID) {
        try {
            signaleringService.deleteZaakSignaleringenOfFormerBehandelaren(zaakUUID).also {
                LOG.info(FormerBehandelaarSignaleringenDeleted(zaakUuid = zaakUUID, signaleringCount = it))
            }
        } catch (notFoundException: NotFoundException) {
            LOG.fine(ZaakEvent.ZaakOfDeletedRolNotFound(zaakUuid = zaakUUID), notFoundException)
        }
    }

    private fun isAuthenticated(headers: HttpHeaders) = secret == headers.getHeaderString(HttpHeaders.AUTHORIZATION)

    /**
     * In case of a 'zaak destroy' notification, delete any Flowable process data related to the zaak,
     * including any related Flowable task data.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleFlowableProcessData(notification: Notification) {
        try {
            if (notification.channel == Channel.ZAKEN && notification.resource == Resource.ZAAK && notification.action == Action.DELETE) {
                notification.resourceUrl.extractUuid().let { zaakUUID ->
                    zaakProcessService.deleteInAllEngines(zaakUUID)
                    LOG.info(ZaakEvent.FlowableProcessDataDeleted(zaakUuid = zaakUUID))
                }
            }
        } catch (exception: RuntimeException) {
            warning("flowable process data", notification, exception)
        }
    }

    /**
     * Determines whether the notification is a Dimpact productaanvraag notification and if so,
     * handles it accordingly by creating a zaak and starting a zaakafhandel process.
     * Only attempts to handle a productaanvraag if the notification resource is an object with 'CREATE' action
     * and has an object type defined.
     */
    private fun handleProductaanvraag(notification: Notification) {
        val objecttypeUri = notification.properties?.let { it[OBJECTTYPE_KENMERK] }
        if (notification.resource == Resource.OBJECT && notification.action == Action.CREATE && objecttypeUri?.isNotEmpty() == true) {
            productaanvraagService.handleProductaanvraag(notification.resourceUrl.extractUuid())
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleWebsockets(notification: Notification) {
        try {
            ScreenEventType.getEvents(
                notification.channel,
                notification.getMainResourceInfo(),
                notification.getResourceInfo()
            ).forEach {
                eventingService.send(it)
            }
        } catch (exception: RuntimeException) {
            warning("websockets", notification, exception)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleSignaleringen(notification: Notification) {
        try {
            // in case of a 'zaak destroy' notification, remove any existing zaak
            // and task signaleringen for this zaak
            if (notification.channel == Channel.ZAKEN &&
                notification.resource == Resource.ZAAK &&
                notification.action == Action.DELETE
            ) {
                deleteSignaleringenForZaak(notification.resourceUrl.extractUuid())
            }
            if (notification.channel == Channel.ZAKEN &&
                notification.resource == Resource.ROL &&
                notification.action == Action.DELETE
            ) {
                notification.mainResourceUrl.extractUuid().let(::deleteZaakSignaleringenOfFormerBehandelaren)
            }
            // send signalering events for this notification
            getSignaleringEvents(
                notification.channel,
                notification.getMainResourceInfo(),
                notification.getResourceInfo()
            ).forEach(eventingService::send)
        } catch (exception: RuntimeException) {
            warning("signaleringen", notification, exception)
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition", "TooGenericExceptionCaught")
    private fun handleIndexing(notification: Notification) {
        try {
            when (notification.channel) {
                Channel.ZAKEN -> {
                    when (notification.resource) {
                        Resource.ZAAK -> {
                            val zaakUUID = notification.resourceUrl.extractUuid()
                            when (notification.action) {
                                Action.CREATE, Action.UPDATE -> indexingService.addOrUpdateZaak(
                                    zaakUUID,
                                    notification.action == Action.UPDATE
                                )
                                Action.DELETE -> {
                                    indexingService.removeZaak(zaakUUID)
                                    taskService.listTasksForZaak(zaakUUID).forEach {
                                        indexingService.removeTaak(it.id)
                                    }
                                }
                                else -> {}
                            }
                        }
                        Resource.STATUS, Resource.RESULTAAT, Resource.ZAAKOBJECT -> {
                            indexingService.addOrUpdateZaak(notification.mainResourceUrl.extractUuid(), false)
                        }
                        Resource.ROL -> handleRolIndexing(notification.mainResourceUrl.extractUuid())
                        Resource.ZAAKEIGENSCHAP -> handleZaakeigenschapIndexing(notification)
                        Resource.ZAAKINFORMATIEOBJECT -> {
                            if (notification.action == Action.CREATE) {
                                indexingService.addOrUpdateInformatieobjectByZaakinformatieobject(
                                    notification.resourceUrl.extractUuid()
                                )
                            }
                        }
                        else -> {}
                    }
                }
                Channel.INFORMATIEOBJECTEN -> {
                    if (notification.resource == Resource.INFORMATIEOBJECT) {
                        val informatieobjectUUID = notification.resourceUrl.extractUuid()
                        when (notification.action) {
                            Action.CREATE, Action.UPDATE -> indexingService.addOrUpdateInformatieobject(
                                informatieobjectUUID
                            )
                            Action.DELETE -> indexingService.removeInformatieobject(informatieobjectUUID)
                            else -> {}
                        }
                    }
                }
                else -> {}
            }
        } catch (exception: RuntimeException) {
            warning("indexing", notification, exception)
        }
    }

    /**
     * Reindexes the zaak and, when it is zaakspecifiek geautoriseerd, its taken and - asynchronously -
     * its documenten, because a behandelaar change moves the geautoriseerde medewerker that those
     * taak- and document-rows carry a copy of. Without this the previous behandelaar would keep passing
     * the Solr exclusion on those rows while the new one is omitted from them.
     */
    private fun handleRolIndexing(zaakUUID: UUID) {
        indexingService.addOrUpdateZaak(zaakUUID, false)
        if (zrcClientService.isZaakspecifiekGeautoriseerd(zaakUUID)) {
            indexingService.addOrUpdateTakenForZaak(zaakUUID)
            indexingService.addOrUpdateInformatieobjectenForZaakAsync(zaakUUID)
        }
    }

    /**
     * Reindexes the zaak, its taken and, asynchronously, its documenten, but only when the
     * changed zaakeigenschap is ZAAK_GEAUTORISEERD: a zaak can have many eigenschappen that change
     * far more often than its zaakspecifiek geautoriseerd status, and reindexing the zaak's
     * documenten requires a ZGW call per document, so reindexing on every zaakeigenschap
     * notificatie would be expensive on a path that Open Notificaties retries on
     * timeout. A 'destroy' notificatie cannot be read back to check its naam, so it always triggers
     * a reindex, as does a zaakeigenschap that cannot be read back for any other reason: whether it
     * has already been deleted (a 404) or the read itself failed (e.g. a ZGW timeout or 5xx), the safe
     * default is to reindex anyway, since a transient read failure only costs one extra reindex, while
     * silently skipping it would leave the zaak out of sync with Solr with nothing left to retry, as
     * this handler already reports success back to Open Notificaties. Because the documenten are
     * reindexed asynchronously, there is a short window where the zaak (and its taken) already reflect
     * the new zaakspecifiek geautoriseerd status but its documenten do not yet.
     */
    private fun handleZaakeigenschapIndexing(notification: Notification) {
        val zaakUUID = notification.mainResourceUrl.extractUuid()
        if (notification.action != Action.DELETE &&
            !isZaakeigenschapGeautoriseerdOrUnreadable(notification, zaakUUID)
        ) {
            return
        }
        indexingService.addOrUpdateZaakWithTaken(zaakUUID)
        indexingService.addOrUpdateInformatieobjectenForZaakAsync(zaakUUID)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun isZaakeigenschapGeautoriseerdOrUnreadable(notification: Notification, zaakUUID: UUID) =
        try {
            zrcClientService.readZaakeigenschap(zaakUUID, notification.resourceUrl.extractUuid()).naam ==
                ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD
        } catch (exception: RuntimeException) {
            LOG.warning(ZaakEvent.ZaakeigenschapReadFailed(zaakUuid = zaakUUID), exception)
            true
        }

    @Suppress("TooGenericExceptionCaught")
    private fun handleInboxDocuments(notification: Notification) {
        // Used by "Abonnementen" functionality in OpenNotificaties to check if callback URL is active
        if (notification.channel == Channel.TEST) {
            LOG.info(TestNotificationReceived)
            return
        }
        try {
            if (notification.action == Action.CREATE) {
                when (notification.resource) {
                    Resource.INFORMATIEOBJECT -> inboxDocumentService.create(
                        notification.resourceUrl.extractUuid()
                    )
                    Resource.ZAAKINFORMATIEOBJECT -> inboxDocumentService.deleteForZaakinformatieobject(
                        notification.resourceUrl.extractUuid()
                    )
                    else -> {}
                }
            }
            if (notification.action == Action.DELETE && notification.resource == Resource.INFORMATIEOBJECT) {
                inboxDocumentService.deleteIfExists(notification.resourceUrl.extractUuid())
            }
        } catch (exception: RuntimeException) {
            warning("inbox documents", notification, exception)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleDetachedDocuments(notification: Notification) {
        // Used by "Abonnementen" functionality in OpenNotificaties to check if callback URL is active
        if (notification.channel == Channel.TEST) return
        if (notification.action != Action.DELETE || notification.resource != Resource.INFORMATIEOBJECT) return
        try {
            detachedDocumentService.deleteIfExists(notification.resourceUrl.extractUuid())
        } catch (exception: RuntimeException) {
            warning("detached documents", notification, exception)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleZaaktype(notification: Notification) {
        if (notification.resource != Resource.ZAAKTYPE) return
        try {
            if (notification.action == Action.CREATE || notification.action == Action.UPDATE) {
                zaaktypeConfigurationBeheerService.updateZaaktypeConfiguration(notification.resourceUrl)
                eventingService.send(ScreenEventType.ZAAKTYPE_CONFIGURATIES.updated(FixedScreenEventResourceId.ZAAKTYPE_CONFIGURATIES))
            }
        } catch (exception: RuntimeException) {
            warning("zaaktype", notification, exception)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleBesluittypeAndInformatieobjecttype(notification: Notification) {
        try {
            when (notification.resource) {
                Resource.BESLUITTYPE -> ztcClientService.invalidateBesluittype(
                    besluittypeURI = notification.resourceUrl,
                    isDeleted = notification.action == Action.DELETE
                )
                Resource.INFORMATIEOBJECTTYPE -> ztcClientService.invalidateInformatieobjecttype(
                    notification.resourceUrl
                )
                else -> {}
            }
        } catch (exception: RuntimeException) {
            warning("besluittype and informatieobjecttype", notification, exception)
        }
    }

    private fun warning(handler: String, notification: Notification, exception: RuntimeException) =
        LOG.warning(
            NotificationHandlingFailed(
                handler = handler,
                channel = notification.channel,
                resource = notification.resource,
                resourceUrl = notification.resourceUrl,
                action = notification.action
            ),
            exception
        )

    private fun Notification.toNotificationReceived() = NotificationReceived(
        channel = channel,
        mainResourceUrl = mainResourceUrl,
        resource = resource,
        resourceUrl = resourceUrl,
        action = action,
        creationDateTime = creationDateTime
    )
}
