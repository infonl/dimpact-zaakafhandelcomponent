/*
 * SPDX-FileCopyrightText: 2021 Atos
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
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor
import org.eclipse.microprofile.config.inject.ConfigProperty
import java.util.function.Supplier
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
        setFunctioneelGebruiker(httpSession.get()!!)
        LOG.info(Supplier { "Received notification: '$notification'" })
        handleSignaleringen(notification)
        handleProductaanvraag(notification)
        handleIndexering(notification)
        handleInboxDocumenten(notification)
        handleZaaktype(notification)
        handleWebsockets(notification)
        return Response.noContent().build()
    }

    private fun isAuthenticated(headers: HttpHeaders) = secret == headers.getHeaderString(HttpHeaders.AUTHORIZATION)

    private fun handleProductaanvraag(notification: Notification) {
        val objecttypeUri = notification.properties[OBJECTTYPE_KENMERK]
        // only attempt to handle productaanvraag if the notification resource is an object with 'CREATE' action
        // and has an object type defined
        if (notification.resource == Resource.OBJECT && notification.action == Action.CREATE && objecttypeUri?.isNotEmpty() == true) {
            productaanvraagService.handleProductaanvraag(notification.resourceUrl!!.extractUuid())
        }
    }

    private fun handleWebsockets(notification: Notification) {
        try {
            if (notification.channel != null && notification.resource != null) {
                ScreenEventType.getEvents(
                    notification.channel,
                    notification.getMainResourceInfo(),
                    notification.getResourceInfo()
                ).forEach(eventingService::send)
            }
        } catch (exception: RuntimeException) {
            warning("Websockets", notification, exception)
        }
    }

    private fun handleSignaleringen(notification: Notification) {
        try {
            if (notification.channel != null && notification.resource != null) {
                SignaleringEventUtil.getEvents(
                    notification.channel,
                    notification.getMainResourceInfo(),
                    notification.getResourceInfo()
                ).forEach(eventingService::send)
            }
        } catch (exception: RuntimeException) {
            warning("Signaleringen", notification, exception)
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun handleIndexering(notification: Notification) {
        try {
            if (notification.channel == Channel.ZAKEN) {
                if (notification.resource == Resource.ZAAK) {
                    if (notification.action == Action.CREATE || notification.action == Action.UPDATE) {
                        indexingService.addOrUpdateZaak(
                            notification.resourceUrl!!.extractUuid(),
                            notification.action == Action.UPDATE
                        )
                    } else if (notification.action == Action.DELETE) {
                        indexingService.removeZaak(notification.resourceUrl!!.extractUuid())
                    }
                } else if (notification.resource == Resource.STATUS || notification.resource == Resource.RESULTAAT ||
                    notification.resource == Resource.ROL || notification.resource == Resource.ZAAKOBJECT
                ) {
                    indexingService.addOrUpdateZaak(notification.mainResourceUrl!!.extractUuid(), false)
                } else if (notification.resource == Resource.ZAAKINFORMATIEOBJECT && notification.action == Action.CREATE) {
                    indexingService.addOrUpdateInformatieobjectByZaakinformatieobject(
                        notification.resourceUrl!!.extractUuid()
                    )
                }
            }
            if (notification.channel == Channel.INFORMATIEOBJECTEN) {
                if (notification.resource == Resource.INFORMATIEOBJECT) {
                    if (notification.action == Action.CREATE || notification.action == Action.UPDATE) {
                        indexingService.addOrUpdateInformatieobject(notification.resourceUrl!!.extractUuid())
                    } else if (notification.action == Action.DELETE) {
                        indexingService.removeInformatieobject(notification.resourceUrl!!.extractUuid())
                    }
                }
            }
        } catch (exception: RuntimeException) {
            warning("Indexering", notification, exception)
        }
    }

    private fun handleInboxDocumenten(notification: Notification) {
        try {
            if (notification.action == Action.CREATE) {
                if (notification.resource == Resource.INFORMATIEOBJECT) {
                    inboxDocumentenService.create(notification.resourceUrl!!.extractUuid())
                } else if (notification.resource == Resource.ZAAKINFORMATIEOBJECT) {
                    inboxDocumentenService.delete(notification.resourceUrl!!.extractUuid())
                }
            }
        } catch (exception: RuntimeException) {
            warning("InboxDocumenten", notification, exception)
        }
    }

    private fun handleZaaktype(notification: Notification) {
        if (notification.resource != Resource.ZAAKTYPE) return
        try {
            if (notification.action == Action.CREATE || notification.action == Action.UPDATE) {
                zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(notification.resourceUrl!!)
            }
        } catch (exception: RuntimeException) {
            warning("Zaaktype", notification, exception)
        }
    }

    private fun warning(handler: String, notification: Notification, exception: RuntimeException) =
        LOG.log(
            Level.WARNING,
            "Failed to handle notification '$notification' in handler '$handler'",
            exception
        )
}
