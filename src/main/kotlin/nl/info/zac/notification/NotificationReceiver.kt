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
import net.atos.client.zgw.util.extractUuid
import net.atos.zac.admin.ZaakafhandelParameterBeheerService
import net.atos.zac.authentication.ActiveSession
import net.atos.zac.authentication.setFunctioneelGebruiker
import net.atos.zac.documenten.InboxDocumentenService
import net.atos.zac.event.EventingService
import net.atos.zac.productaanvraag.ProductaanvraagService
import net.atos.zac.signalering.event.SignaleringEventUtil
import net.atos.zac.websocket.event.ScreenEventType
import net.atos.zac.zoeken.IndexingService
import nl.info.zac.flowable.cmmn.CMMNService
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
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
@Suppress("LongParameterList")
class NotificationReceiver @Inject constructor(
    private val eventingService: EventingService,
    private val productaanvraagService: ProductaanvraagService,
    private val indexingService: IndexingService,
    private val inboxDocumentenService: InboxDocumentenService,
    private val zaakafhandelParameterBeheerService: ZaakafhandelParameterBeheerService,
    private val cmmnService: CMMNService,

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

    private fun isAuthenticated(headers: HttpHeaders) = secret == headers.getHeaderString(HttpHeaders.AUTHORIZATION)

    /**
     * In case of a 'zaak destroy' notification, delete any Flowable process data related to the zaak,
     * including any related Flowable task data.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun handleFlowableProcessData(notification: Notification) {
        try {
            if (notification.channel == Channel.ZAKEN && notification.resource == Resource.ZAAK && notification.action == Action.DELETE) {
                cmmnService.deleteCase(notification.resourceUrl.extractUuid())
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
            ).forEach(eventingService::send)
        } catch (exception: RuntimeException) {
            warning("websockets", notification, exception)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun handleSignaleringen(notification: Notification) {
        try {
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
                            when (notification.action) {
                                Action.CREATE, Action.UPDATE -> indexingService.addOrUpdateZaak(
                                    notification.resourceUrl.extractUuid(),
                                    notification.action == Action.UPDATE
                                )
                                Action.DELETE -> indexingService.removeZaak(notification.resourceUrl.extractUuid())
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
                        when (notification.action) {
                            Action.CREATE, Action.UPDATE -> indexingService.addOrUpdateInformatieobject(
                                notification.resourceUrl.extractUuid()
                            )
                            Action.DELETE -> indexingService.removeInformatieobject(
                                notification.resourceUrl.extractUuid()
                            )
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
                when (notification.resource) {
                    Resource.INFORMATIEOBJECT -> inboxDocumentenService.create(
                        notification.resourceUrl.extractUuid()
                    )
                    Resource.ZAAKINFORMATIEOBJECT -> inboxDocumentenService.delete(
                        notification.resourceUrl.extractUuid()
                    )
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
