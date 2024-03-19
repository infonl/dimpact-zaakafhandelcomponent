/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.notificaties;

import static jakarta.ws.rs.core.Response.noContent;
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
import static net.atos.zac.util.UriUtil.uuidFromURI;
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
import net.atos.client.or.objecttype.model.Objecttype;
import net.atos.zac.aanvraag.ProductaanvraagService;
import net.atos.zac.authentication.ActiveSession;
import net.atos.zac.authentication.SecurityUtil;
import net.atos.zac.configuratie.ConfiguratieService;
import net.atos.zac.documenten.InboxDocumentenService;
import net.atos.zac.event.EventingService;
import net.atos.zac.signalering.event.SignaleringEventUtil;
import net.atos.zac.websocket.event.ScreenEventType;
import net.atos.zac.zaaksturing.ZaakafhandelParameterBeheerService;
import net.atos.zac.zoeken.IndexeerService;

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
    private static final String PRODUCTAANVRAAG_DIMPACT_OBJECTTYPE_NAAM = "Productaanvraag-Dimpact";

    private EventingService eventingService;
    private ProductaanvraagService productaanvraagService;
    private ConfiguratieService configuratieService;
    private IndexeerService indexeerService;
    private InboxDocumentenService inboxDocumentenService;
    private ZaakafhandelParameterBeheerService zaakafhandelParameterBeheerService;
    private ObjecttypesClientService objecttypesClientService;
    private String secret;
    private Instance<HttpSession> httpSession;

    /**
     * Empty no-op constructor as required by JAX-RS.
     */
    public NotificatieReceiver() {
    }

    @Inject
    public NotificatieReceiver(
            EventingService eventingService,
            ProductaanvraagService productaanvraagService,
            ConfiguratieService configuratieService,
            IndexeerService indexeerService,
            InboxDocumentenService inboxDocumentenService,
            ZaakafhandelParameterBeheerService zaakafhandelParameterBeheerService,
            ObjecttypesClientService objecttypesClientService,
            @ConfigProperty(name = "OPEN_NOTIFICATIONS_API_SECRET_KEY") String secret,
            @ActiveSession Instance<HttpSession> httpSession
    ) {
        this.eventingService = eventingService;
        this.productaanvraagService = productaanvraagService;
        this.configuratieService = configuratieService;
        this.indexeerService = indexeerService;
        this.inboxDocumentenService = inboxDocumentenService;
        this.zaakafhandelParameterBeheerService = zaakafhandelParameterBeheerService;
        this.objecttypesClientService = objecttypesClientService;
        this.secret = secret;
        this.httpSession = httpSession;
    }

    @POST
    public Response notificatieReceive(@Context HttpHeaders headers, final Notificatie notificatie) {
        SecurityUtil.setFunctioneelGebruiker(httpSession.get());
        if (isAuthenticated(headers)) {
            LOG.info(() -> "Notificatie ontvangen: %s"
                    .formatted(notificatie.toString()));
            handleWebsockets(notificatie);
            if (!configuratieService.isLocalDevelopment()) {
                handleSignaleringen(notificatie);
                handleProductaanvraag(notificatie);
                handleIndexering(notificatie);
                handleInboxDocumenten(notificatie);
                handleZaaktype(notificatie);
            }
            return noContent().build();
        } else {
            return noContent().status(Response.Status.FORBIDDEN).build();
        }
    }

    private boolean isAuthenticated(final HttpHeaders headers) {
        return secret.equals(headers.getHeaderString(HttpHeaders.AUTHORIZATION));
    }

    private void handleWebsockets(final Notificatie notificatie) {
        try {
            if (notificatie.getChannel() != null && notificatie.getResource() != null) {
                ScreenEventType.getEvents(notificatie.getChannel(), notificatie.getMainResourceInfo(),
                        notificatie.getResourceInfo())
                        .forEach(eventingService::send);
            }
        } catch (RuntimeException ex) {
            warning("Websockets", notificatie, ex);
        }
    }

    private void handleSignaleringen(final Notificatie notificatie) {
        try {
            if (notificatie.getChannel() != null && notificatie.getResource() != null) {
                SignaleringEventUtil.getEvents(notificatie.getChannel(), notificatie.getMainResourceInfo(),
                        notificatie.getResourceInfo())
                        .forEach(eventingService::send);
            }
        } catch (RuntimeException ex) {
            warning("Signaleringen", notificatie, ex);
        }
    }

    private void handleProductaanvraag(final Notificatie notificatie) {
        try {
            if (isProductaanvraagDimpact(notificatie)) {
                LOG.info(() -> "Verwerken productaanvraag Dimpact: %s"
                        .formatted(notificatie.toString()));
                productaanvraagService.verwerkProductaanvraag(notificatie.getResourceUrl());
            }
        } catch (RuntimeException ex) {
            warning("Productaanvraag", notificatie, ex);
        }
    }

    private boolean isProductaanvraagDimpact(final Notificatie notificatie) {
        final String producttypeUri = notificatie.getProperties().get(OBJECTTYPE_KENMERK);
        if (notificatie.getResource() != OBJECT || notificatie.getAction() != CREATE || isEmpty(producttypeUri)) {
            return false;
        }
        final Objecttype objecttype = objecttypesClientService.readObjecttype(uuidFromURI(producttypeUri));
        return PRODUCTAANVRAAG_DIMPACT_OBJECTTYPE_NAAM.equals(objecttype.getName());
    }

    private void handleIndexering(final Notificatie notificatie) {
        try {
            if (notificatie.getChannel() == Channel.ZAKEN) {
                if (notificatie.getResource() == ZAAK) {
                    if (notificatie.getAction() == CREATE || notificatie.getAction() == UPDATE) {
                        // Updaten van taak is nodig bij afsluiten zaak
                        indexeerService.addOrUpdateZaak(uuidFromURI(notificatie.getResourceUrl()),
                                notificatie.getAction() == UPDATE);
                    } else if (notificatie.getAction() == DELETE) {
                        indexeerService.removeZaak(uuidFromURI(notificatie.getResourceUrl()));
                    }
                } else if (notificatie.getResource() == STATUS || notificatie.getResource() == RESULTAAT ||
                           notificatie.getResource() == ROL || notificatie.getResource() == ZAAKOBJECT) {
                    indexeerService.addOrUpdateZaak(uuidFromURI(notificatie.getMainResourceUrl()), false);
                } else if (notificatie.getResource() == ZAAKINFORMATIEOBJECT && notificatie.getAction() == CREATE) {
                    indexeerService.addOrUpdateInformatieobjectByZaakinformatieobject(
                            uuidFromURI(notificatie.getResourceUrl()));
                }
            }
            if (notificatie.getChannel() == Channel.INFORMATIEOBJECTEN) {
                if (notificatie.getResource() == INFORMATIEOBJECT) {
                    if (notificatie.getAction() == CREATE || notificatie.getAction() == UPDATE) {
                        indexeerService.addOrUpdateInformatieobject(uuidFromURI(notificatie.getResourceUrl()));
                    } else if (notificatie.getAction() == DELETE) {
                        indexeerService.removeInformatieobject(uuidFromURI(notificatie.getResourceUrl()));
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
                    inboxDocumentenService.create(uuidFromURI(notificatie.getResourceUrl()));
                } else if (notificatie.getResource() == ZAAKINFORMATIEOBJECT) {
                    inboxDocumentenService.delete(uuidFromURI(notificatie.getResourceUrl()));
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
