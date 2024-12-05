/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.notificaties;

import static jakarta.ws.rs.core.Response.noContent;
import static net.atos.client.zgw.util.UriUtilsKt.extractUuid;
import static net.atos.zac.authentication.SecurityUtilKt.setFunctioneelGebruiker;
import static net.atos.zac.notificaties.Action.CREATE;
import static net.atos.zac.notificaties.Action.DELETE;
import static net.atos.zac.notificaties.Action.UPDATE;
import static net.atos.zac.notificaties.Resource.INFORMATIEOBJECT;
import static net.atos.zac.notificaties.Resource.OBJECT;
import static net.atos.zac.notificaties.Resource.RESULTAAT;
import static net.atos.zac.notificaties.Resource.ROL;
import static net.atos.zac.notificaties.Resource.STATUS;
import static net.atos.zac.notificaties.Resource.ZAAK;
import static net.atos.zac.notificaties.Resource.ZAAKINFORMATIEOBJECT;
import static net.atos.zac.notificaties.Resource.ZAAKOBJECT;
import static net.atos.zac.notificaties.Resource.ZAAKTYPE;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import net.atos.client.or.objecttype.ObjecttypesClientService;
import net.atos.zac.admin.ZaakafhandelParameterBeheerService;
import net.atos.zac.authentication.ActiveSession;
import net.atos.zac.documenten.InboxDocumentenService;
import net.atos.zac.event.EventingService;
import net.atos.zac.productaanvraag.ProductaanvraagService;
import net.atos.zac.signalering.event.SignaleringEventUtil;
import net.atos.zac.websocket.event.ScreenEventType;
import net.atos.zac.zoeken.IndexingService;

/**
 * Provides REST endpoints for receiving notifications about events that ZAC needs to know about
 * so that it can take appropriate action.
 */
@Path("notificaties")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class NotificatieReceiver {
    private static final Logger LOG = Logger.getLogger(NotificatieReceiver.class.getName());
    private static final String OBJECTTYPE_KENMERK = "objectType";

    private EventingService eventingService;
    private ProductaanvraagService productaanvraagService;
    private IndexingService indexingService;
    private InboxDocumentenService inboxDocumentenService;
    private ZaakafhandelParameterBeheerService zaakafhandelParameterBeheerService;
    private String secret;
    private Instance<HttpSession> httpSession;

    /**
     * Empty no-op constructor as required by Weld.
     */
    public NotificatieReceiver() {
    }

    @Inject
    public NotificatieReceiver(
            EventingService eventingService,
            ProductaanvraagService productaanvraagService,
            IndexingService indexingService,
            InboxDocumentenService inboxDocumentenService,
            ZaakafhandelParameterBeheerService zaakafhandelParameterBeheerService,
            ObjecttypesClientService objecttypesClientService,
            @ConfigProperty(name = "OPEN_NOTIFICATIONS_API_SECRET_KEY") String secret,
            @ActiveSession Instance<HttpSession> httpSession
    ) {
        this.eventingService = eventingService;
        this.productaanvraagService = productaanvraagService;
        this.indexingService = indexingService;
        this.inboxDocumentenService = inboxDocumentenService;
        this.zaakafhandelParameterBeheerService = zaakafhandelParameterBeheerService;
        this.secret = secret;
        this.httpSession = httpSession;
    }

    @POST
    public Response notificatieReceive(@Context HttpHeaders headers, final Notificatie notificatie) {
        if (!isAuthenticated(headers)) {
            return noContent().status(Response.Status.FORBIDDEN).build();
        }
        setFunctioneelGebruiker(httpSession.get());
        LOG.info(() -> "Notificatie ontvangen: %s".formatted(notificatie.toString()));
        handleSignaleringen(notificatie);
        handleProductaanvraag(notificatie);
        handleIndexering(notificatie);
        handleInboxDocumenten(notificatie);
        handleZaaktype(notificatie);
        handleWebsockets(notificatie);
        return noContent().build();
    }

    private boolean isAuthenticated(final HttpHeaders headers) {
        return secret.equals(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    private void handleProductaanvraag(final Notificatie notificatie) {
        final String objecttypeUri = notificatie.getProperties().get(OBJECTTYPE_KENMERK);
        // only attempt to handle productaanvraag if the notification resource is an object with 'CREATE' action
        // and has an object type defined
        if (notificatie.getResource() == OBJECT && notificatie.getAction() == CREATE && !isEmpty(objecttypeUri)) {
            productaanvraagService.handleProductaanvraag(extractUuid(notificatie.getResourceUrl()));
        }
    }

    private void handleWebsockets(final Notificatie notificatie) {
        try {
            if (notificatie.getChannel() != null && notificatie.getResource() != null) {
                ScreenEventType.getEvents(
                        notificatie.getChannel(),
                        notificatie.getMainResourceInfo(),
                        notificatie.getResourceInfo()
                ).forEach(eventingService::send);
            }
        } catch (RuntimeException ex) {
            warning("Websockets", notificatie, ex);
        }
    }

    private void handleSignaleringen(final Notificatie notificatie) {
        try {
            if (notificatie.getChannel() != null && notificatie.getResource() != null) {
                SignaleringEventUtil.getEvents(
                        notificatie.getChannel(),
                        notificatie.getMainResourceInfo(),
                        notificatie.getResourceInfo()
                ).forEach(eventingService::send);
            }
        } catch (RuntimeException ex) {
            warning("Signaleringen", notificatie, ex);
        }
    }

    private void handleIndexering(final Notificatie notificatie) {
        try {
            if (notificatie.getChannel() == Channel.ZAKEN) {
                if (notificatie.getResource() == ZAAK) {
                    if (notificatie.getAction() == CREATE || notificatie.getAction() == UPDATE) {
                        // Updaten van taak is nodig bij afsluiten zaak
                        indexingService.addOrUpdateZaak(extractUuid(notificatie.getResourceUrl()),
                                notificatie.getAction() == UPDATE);
                    } else if (notificatie.getAction() == DELETE) {
                        indexingService.removeZaak(extractUuid(notificatie.getResourceUrl()));
                    }
                } else if (notificatie.getResource() == STATUS || notificatie.getResource() == RESULTAAT ||
                           notificatie.getResource() == ROL || notificatie.getResource() == ZAAKOBJECT) {
                    indexingService.addOrUpdateZaak(extractUuid(notificatie.getMainResourceUrl()), false);
                } else if (notificatie.getResource() == ZAAKINFORMATIEOBJECT && notificatie.getAction() == CREATE) {
                    indexingService.addOrUpdateInformatieobjectByZaakinformatieobject(
                            extractUuid(notificatie.getResourceUrl()));
                }
            }
            if (notificatie.getChannel() == Channel.INFORMATIEOBJECTEN) {
                if (notificatie.getResource() == INFORMATIEOBJECT) {
                    if (notificatie.getAction() == CREATE || notificatie.getAction() == UPDATE) {
                        indexingService.addOrUpdateInformatieobject(extractUuid(notificatie.getResourceUrl()));
                    } else if (notificatie.getAction() == DELETE) {
                        indexingService.removeInformatieobject(extractUuid(notificatie.getResourceUrl()));
                    }
                }
            }
        } catch (RuntimeException ex) {
            warning("Indexering", notificatie, ex);
        }
    }

    private void handleInboxDocumenten(final Notificatie notificatie) {
        try {
            if (notificatie.getAction() == CREATE) {
                if (notificatie.getResource() == INFORMATIEOBJECT) {
                    inboxDocumentenService.create(extractUuid(notificatie.getResourceUrl()));
                } else if (notificatie.getResource() == ZAAKINFORMATIEOBJECT) {
                    inboxDocumentenService.delete(extractUuid(notificatie.getResourceUrl()));
                }
            }
        } catch (RuntimeException ex) {
            warning("InboxDocumenten", notificatie, ex);
        }
    }

    private void handleZaaktype(final Notificatie notificatie) {
        try {
            if (notificatie.getResource() == ZAAKTYPE) {
                if (notificatie.getAction() == CREATE || notificatie.getAction() == UPDATE) {
                    zaakafhandelParameterBeheerService.zaaktypeAangepast(notificatie.getResourceUrl());
                }
            }
        } catch (RuntimeException ex) {
            warning("Zaaktype", notificatie, ex);
        }
    }

    private void warning(final String handler, final Notificatie notificatie, final RuntimeException ex) {
        LOG.log(Level.WARNING,
                "Er is iets fout gegaan in de %s-handler bij het afhandelen van notificatie: %s"
                        .formatted(handler, notificatie.toString()),
                ex);
    }
}
