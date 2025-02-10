/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.notificaties

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
class NotificatieReceiver @Inject constructor(
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
        private val LOG = Logger.getLogger(NotificatieReceiver::class.java.getName())
        private const val OBJECTTYPE_KENMERK = "objectType"
    }

    @POST
    fun notificatieReceive(@Context headers: HttpHeaders, notificatie: Notificatie): Response {
        if (!isAuthenticated(headers)) {
            return Response.noContent().status(Response.Status.FORBIDDEN).build()
        }
        setFunctioneelGebruiker(httpSession.get()!!)
        LOG.info(Supplier { "Received notification: '$notificatie'" })
        handleSignaleringen(notificatie)
        handleProductaanvraag(notificatie)
        handleIndexering(notificatie)
        handleInboxDocumenten(notificatie)
        handleZaaktype(notificatie)
        handleWebsockets(notificatie)
        return Response.noContent().build()
    }

    private fun isAuthenticated(headers: HttpHeaders) = secret == headers.getHeaderString(HttpHeaders.AUTHORIZATION)

    private fun handleProductaanvraag(notificatie: Notificatie) {
        val objecttypeUri = notificatie.properties[OBJECTTYPE_KENMERK]
        // only attempt to handle productaanvraag if the notification resource is an object with 'CREATE' action
        // and has an object type defined
        if (notificatie.resource == Resource.OBJECT && notificatie.action == Action.CREATE && objecttypeUri?.isNotEmpty() == true) {
            productaanvraagService.handleProductaanvraag(notificatie.resourceUrl!!.extractUuid())
        }
    }

    private fun handleWebsockets(notificatie: Notificatie) {
        try {
            if (notificatie.channel != null && notificatie.resource != null) {
                ScreenEventType.getEvents(
                    notificatie.channel,
                    notificatie.getMainResourceInfo(),
                    notificatie.getResourceInfo()
                ).forEach(eventingService::send)
            }
        } catch (exception: RuntimeException) {
            warning("Websockets", notificatie, exception)
        }
    }

    private fun handleSignaleringen(notificatie: Notificatie) {
        try {
            if (notificatie.channel != null && notificatie.resource != null) {
                SignaleringEventUtil.getEvents(
                    notificatie.channel,
                    notificatie.getMainResourceInfo(),
                    notificatie.getResourceInfo()
                ).forEach(eventingService::send)
            }
        } catch (exception: RuntimeException) {
            warning("Signaleringen", notificatie, exception)
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "ComplexCondition")
    private fun handleIndexering(notificatie: Notificatie) {
        try {
            if (notificatie.channel == Channel.ZAKEN) {
                if (notificatie.resource == Resource.ZAAK) {
                    if (notificatie.action == Action.CREATE || notificatie.action == Action.UPDATE) {
                        indexingService.addOrUpdateZaak(
                            notificatie.resourceUrl!!.extractUuid(),
                            notificatie.action == Action.UPDATE
                        )
                    } else if (notificatie.action == Action.DELETE) {
                        indexingService.removeZaak(notificatie.resourceUrl!!.extractUuid())
                    }
                } else if (notificatie.resource == Resource.STATUS || notificatie.resource == Resource.RESULTAAT ||
                    notificatie.resource == Resource.ROL || notificatie.resource == Resource.ZAAKOBJECT
                ) {
                    indexingService.addOrUpdateZaak(notificatie.mainResourceUrl!!.extractUuid(), false)
                } else if (notificatie.resource == Resource.ZAAKINFORMATIEOBJECT && notificatie.action == Action.CREATE) {
                    indexingService.addOrUpdateInformatieobjectByZaakinformatieobject(
                        notificatie.resourceUrl!!.extractUuid()
                    )
                }
            }
            if (notificatie.channel == Channel.INFORMATIEOBJECTEN) {
                if (notificatie.resource == Resource.INFORMATIEOBJECT) {
                    if (notificatie.action == Action.CREATE || notificatie.action == Action.UPDATE) {
                        indexingService.addOrUpdateInformatieobject(notificatie.resourceUrl!!.extractUuid())
                    } else if (notificatie.action == Action.DELETE) {
                        indexingService.removeInformatieobject(notificatie.resourceUrl!!.extractUuid())
                    }
                }
            }
        } catch (exception: RuntimeException) {
            warning("Indexering", notificatie, exception)
        }
    }

    private fun handleInboxDocumenten(notificatie: Notificatie) {
        try {
            if (notificatie.action == Action.CREATE) {
                if (notificatie.resource == Resource.INFORMATIEOBJECT) {
                    inboxDocumentenService.create(notificatie.resourceUrl!!.extractUuid())
                } else if (notificatie.resource == Resource.ZAAKINFORMATIEOBJECT) {
                    inboxDocumentenService.delete(notificatie.resourceUrl!!.extractUuid())
                }
            }
        } catch (exception: RuntimeException) {
            warning("InboxDocumenten", notificatie, exception)
        }
    }

    private fun handleZaaktype(notificatie: Notificatie) {
        if (notificatie.resource != Resource.ZAAKTYPE) return
        try {
            if (notificatie.action == Action.CREATE || notificatie.action == Action.UPDATE) {
                zaakafhandelParameterBeheerService.upsertZaakafhandelParameters(notificatie.resourceUrl!!)
            }
        } catch (exception: RuntimeException) {
            warning("Zaaktype", notificatie, exception)
        }
    }

    private fun warning(handler: String, notificatie: Notificatie, exception: RuntimeException) =
        LOG.log(
            Level.WARNING,
            "Failed to handle notification '$notificatie' in handler '$handler'",
            exception
        )
}
