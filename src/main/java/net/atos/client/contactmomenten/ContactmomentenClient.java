/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.contactmomenten;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.contactmomenten.exception.NotFoundExceptionMapper;
import net.atos.client.contactmomenten.exception.RuntimeExceptionMapper;
import net.atos.client.contactmomenten.model.generated.AuditTrail;
import net.atos.client.contactmomenten.model.generated.ContactMoment;
import net.atos.client.contactmomenten.model.generated.ContactmomentList200Response;
import net.atos.client.contactmomenten.util.ContactmomentenClientHeadersFactory;

/**
 * Contactmomenten API
 * <p>
 * Een API om contactmomenten met klanten te registreren of op te vragen.
 */
@RegisterRestClient(configKey = "Contactmomenten-API-Client")
@RegisterClientHeaders(ContactmomentenClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(NotFoundExceptionMapper.class)
@Path("api/v1/contactmomenten")
public interface ContactmomentenClient {
    String X_NLX_LOGRECORD_ID_HEADER = "X-NLX-Logrecord-ID";
    String X_AUDIT_TOELICHTING_HEADER = "X-Audit-Toelichting";

    /**
     * Alle audit trail regels behorend bij de CONTACTMOMENT.
     */
    @GET
    @Path("/{contactmoment_uuid}/audittrail")
    @Produces({"application/json", "application/problem+json"})
    public List<AuditTrail> audittrailList(
            @PathParam("contactmoment_uuid") UUID contactmomentUuid
    ) throws ProcessingException;

    /**
     * Een specifieke audit trail regel opvragen.
     */
    @GET
    @Path("/{contactmoment_uuid}/audittrail/{uuid}")
    @Produces({"application/json", "application/problem+json"})
    public AuditTrail audittrailRead(
            @PathParam("contactmoment_uuid") UUID contactmomentUuid,
            @PathParam("uuid") UUID uuid,
            @HeaderParam("If-None-Match") String ifNoneMatch
    ) throws ProcessingException;

    /**
     * Maak een CONTACTMOMENT aan.
     */
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    public ContactMoment contactmomentCreate(
            @HeaderParam("Content-Type") String contentType,
            ContactMoment contactMoment,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;

    /**
     * Verwijder een CONTACTMOMENT.
     */
    @DELETE
    @Path("/{uuid}")
    @Produces({"application/problem+json"})
    public void contactmomentDelete(
            @PathParam("uuid") UUID uuid,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;

    /**
     * Alle CONTACTMOMENTen opvragen.
     */
    @GET
    @Produces({"application/json", "application/problem+json"})
    public ContactmomentList200Response contactmomentList(
            @QueryParam("vorigContactmoment") URI vorigContactmoment,
            @QueryParam("volgendContactmoment") URI volgendContactmoment,
            @QueryParam("bronorganisatie") String bronorganisatie,
            @QueryParam("registratiedatum") String registratiedatum,
            @QueryParam("registratiedatum__gt") String registratiedatumGt,
            @QueryParam("registratiedatum__gte") String registratiedatumGte,
            @QueryParam("registratiedatum__lt") String registratiedatumLt,
            @QueryParam("registratiedatum__lte") String registratiedatumLte,
            @QueryParam("kanaal") String kanaal,
            @QueryParam("voorkeurskanaal") String voorkeurskanaal,
            @QueryParam("voorkeurstaal") String voorkeurstaal,
            @QueryParam("initiatiefnemer") String initiatiefnemer,
            @QueryParam("medewerker") URI medewerker,
            @QueryParam("ordering") String ordering,
            @QueryParam("page") Integer page
    ) throws ProcessingException;

    /**
     * Werk een CONTACTMOMENT deels bij.
     */
    @PATCH
    @Path("/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    public ContactMoment contactmomentPartialUpdate(
            @PathParam("uuid") UUID uuid,
            @HeaderParam("Content-Type") String contentType,
            ContactMoment contactMoment,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;

    /**
     * Een specifiek CONTACTMOMENT opvragen.
     */
    @GET
    @Path("/{uuid}")
    @Produces({"application/json", "application/problem+json"})
    public ContactMoment contactmomentRead(
            @PathParam("uuid") UUID uuid,
            @HeaderParam("If-None-Match") String ifNoneMatch
    ) throws ProcessingException;

    /**
     * Werk een CONTACTMOMENT in zijn geheel bij.
     */
    @PUT
    @Path("/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    public ContactMoment contactmomentUpdate(
            @PathParam("uuid") UUID uuid,
            @HeaderParam("Content-Type") String contentType,
            ContactMoment contactMoment,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;
}
