/*
 * SPDX-FileCopyrightText: 2021 Atos, 2025 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.notification

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.servlet.http.HttpSession
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import net.atos.zac.admin.ZaakafhandelParameterBeheerService
import net.atos.zac.authentication.ActiveSession
import net.atos.zac.authentication.setFunctioneelGebruiker
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.flowable.ZaakVariabelenService
import net.atos.zac.flowable.cmmn.CMMNService
import net.atos.zac.productaanvraag.ProductaanvraagService
import net.atos.zac.search.IndexingService
import net.atos.zac.signalering.SignaleringService
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.signalering.model.SignaleringSubject
import net.atos.zac.signalering.model.SignaleringVerzondenZoekParameters
import net.atos.zac.signalering.model.SignaleringZoekParameters
import net.atos.zac.task.TaskService
import net.atos.zac.websocket.event.ScreenEventType
import nl.info.client.zgw.util.extractUuid
import nl.info.zac.productaanvraag.ProductaanvraagService
import nl.info.zac.admin.ZaakafhandelParameterBeheerService
import nl.info.zac.authentication.ActiveSession
import nl.info.zac.authentication.setFunctioneelGebruiker
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger

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
    private val inboxDocumentenService: InboxDocumentenService,
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterBeheerService,
    private val cmmnService: CMMNService,
    private val zaakVariabelenService: ZaakVariabelenService,
    private val signaleringService: SignaleringService,
    private val taskService: TaskService,

    @ConfigProperty(name = "OPEN_NOTIFICATIONS_API_SECRET_KEY")
    private val secret: String,

    @ActiveSession
    private val httpSession: Instance<HttpSession>
) {
    companion object {
        private val LOG = Logger.getLogger(NotificationReceiver::class.java.getName())
        private const val OBJECTTYPE_KENMERK = "objectType"
    }

    @POST
    fun notificatieReceive(@Context headers: HttpHeaders, notification: Notification): Response {
        if (!isAuthenticated(headers)) {
            return Response.noContent().status(Response.Status.FORBIDDEN).build()
        }
        setFunctioneelGebruiker(httpSession.get())
        LOG.info { "Received notification: '$notification'" }
        handleSignaleringen(notification)
        handleProductaanvraag(notification)
        handleIndexing(notification)
        handleInboxDocuments(notification)
        handleFlowableProcessData(notification)
        handleZaaktype(notification)
        handleWebsockets(notification)
        return Response.noContent().build()
    }

    /**
     * Deletes zaak and related task signaleringen as well as 'zaak sent (verzonden)' and 'task sent (verzonden)' records
     * for the given zaak UUID.
     */
    private fun deleteSignaleringenForZaak(zaakUUID: UUID) {
        signaleringService.deleteSignaleringen(
            SignaleringZoekParameters(
                SignaleringSubject.ZAAK,
                zaakUUID.toString()
            )
        ).also {
            LOG.info("Deleted $it zaak signaleringen for zaak with UUID '$zaakUUID'.")
        }
        signaleringService.deleteSignaleringVerzonden(
            SignaleringVerzondenZoekParameters(
                SignaleringSubject.ZAAK,
                zaakUUID.toString()
            )
        ).also {
            LOG.info("Deleted $it 'zaak signaleringen verzonden' records for zaak with UUID '$zaakUUID'.")
        }
        taskService.listTasksForZaak(zaakUUID).forEach { task ->
            signaleringService.deleteSignaleringen(
                SignaleringZoekParameters(
                    SignaleringSubject.TAAK,
                    task.id
                )
            ).also {
                LOG.info(
                    "Deleted $it taak signaleringen for task with ID '${task.id}' and zaak with UUID: '$zaakUUID'."
                )
            }
            signaleringService.deleteSignaleringVerzonden(
                SignaleringVerzondenZoekParameters(
                    SignaleringSubject.TAAK,
                    task.id
                )
            ).also {
                LOG.info(
                    """
                        Deleted $it 'taak signaleringen verzonden' records for task with ID '${task.id}' and 
                        zaak with UUID: '$zaakUUID'.
                    """.trimIndent()
                )
            }
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
                    LOG.info { "Deleting Flowable process data for zaak with UUID '$zaakUUID'" }
                    cmmnService.deleteCase(zaakUUID)
                    zaakVariabelenService.deleteAllCaseVariables(zaakUUID)
                    LOG.info { "Successfully deleted Flowable process data for zaak with UUID '$zaakUUID'" }
                }
            }
        } catch (exception: RuntimeException) {
            warning("flowable process data", notification, exception)
        }
    }

    private fun handleProductaanvraag(notification: Notification) {
        val objecttypeUri = notification.properties?.let { it[OBJECTTYPE_KENMERK] }
        // only attempt to handle productaanvraag if the notification resource is an object with 'CREATE' action
        // and has an object type defined
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
            // in case of a 'zaak destroy' notification remove any existing zaak
            // and task signaleringen for this zaak
            if (notification.channel == Channel.ZAKEN && notification.resource == Resource.ZAAK && notification.action == Action.DELETE) {
                deleteSignaleringenForZaak(notification.resourceUrl.extractUuid())
            }
            // send signalering events for this notification
            SignaleringEventUtil.getEvents(
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
                        Resource.STATUS, Resource.RESULTAAT, Resource.ROL, Resource.ZAAKOBJECT -> {
                            indexingService.addOrUpdateZaak(notification.mainResourceUrl.extractUuid(), false)
                        }
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

    @Suppress("TooGenericExceptionCaught")
    private fun handleInboxDocuments(notification: Notification) {
        try {
            if (notification.action == Action.CREATE) {
                val enkelvoudigInformatieobjectUuid = notification.resourceUrl.extractUuid()
                when (notification.resource) {
                    Resource.INFORMATIEOBJECT -> inboxDocumentenService.create(enkelvoudigInformatieobjectUuid)
                    Resource.ZAAKINFORMATIEOBJECT -> inboxDocumentenService.delete(enkelvoudigInformatieobjectUuid)
                    else -> {}
                }
            }
        } catch (exception: RuntimeException) {
            warning("inbox documents", notification, exception)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleZaaktype(notification: Notification) {
        if (notification.resource != Resource.ZAAKTYPE) return
        try {
            if (notification.action == Action.CREATE || notification.action == Action.UPDATE) {
                zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(notification.resourceUrl)
            }
        } catch (exception: RuntimeException) {
            warning("zaaktype", notification, exception)
        }
    }

    private fun warning(handler: String, notification: Notification, exception: RuntimeException) =
        LOG.log(
            Level.WARNING,
            "Failed to handle notification '$notification' in handler '$handler'",
            exception
        )
}
