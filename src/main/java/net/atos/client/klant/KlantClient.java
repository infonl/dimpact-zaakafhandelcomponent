/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.klant;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.BeanParam;
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

import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.klant.exception.KlantRuntimeExceptionMapper;
import net.atos.client.klant.model.AuditTrail;
import net.atos.client.klant.model.Klant;
import net.atos.client.klant.model.KlantList200Response;
import net.atos.client.klant.model.KlantListParameters;
import net.atos.client.klant.util.KlantClientHeadersFactory;

/**
 * Klanten API
 * <p>
 * Een API om zowel klanten te registreren als op te vragen.
 * Een klant is een natuurlijk persoon, niet-natuurlijk persoon (bedrijf) of vestiging waarbij het gaat om niet geverifieerde gegevens.
 */
@RegisterRestClient(configKey = "Klanten-API-Client")
@RegisterClientHeaders(KlantClientHeadersFactory.class)
@RegisterProvider(KlantRuntimeExceptionMapper.class)
@Path("api/v1/klanten")
public interface KlantClient {
    String X_NLX_LOGRECORD_ID_HEADER = "X-NLX-Logrecord-ID";
    String X_AUDIT_TOELICHTING_HEADER = "X-Audit-Toelichting";

    /**
     * Alle audit trail regels behorend bij de KLANT.
     */
    @GET
    @Path("/{klant_uuid}/audittrail")
    @Produces({"application/json", "application/problem+json"})
    List<AuditTrail> audittrailList(@PathParam("klant_uuid") UUID klantUuid) throws ProcessingException;

    /**
     * Een specifieke audit trail regel opvragen.
     */
    @GET
    @Path("/{klant_uuid}/audittrail/{uuid}")
    @Produces({"application/json", "application/problem+json"})
    AuditTrail audittrailRead(
            @PathParam("klant_uuid") UUID klantUuid,
            @PathParam("uuid") UUID uuid,
            @HeaderParam("If-None-Match") String ifNoneMatch
    ) throws ProcessingException;

    /**
     * Maak een KLANT aan.
     */
    @POST
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    Klant klantCreate(
            @HeaderParam("Content-Type") String contentType,
            Klant klant,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;

    /**
     * Verwijder een KLANT.
     */
    @DELETE
    @Path("/{uuid}")
    @Produces({"application/problem+json"})
    void klantDelete(
            @PathParam("uuid") UUID uuid,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;

    /**
     * Alle KLANTen opvragen.
     */
    @GET
    @Produces({"application/json", "application/problem+json"})
    KlantList200Response klantList(
            @BeanParam final KlantListParameters listParameters
    ) throws ProcessingException;

    /**
     * Alle KLANTen asynchroon opvragen.
     */
    @GET
    @Produces({"application/json", "application/problem+json"})
    CompletionStage<KlantList200Response> klantListAsync(
            @BeanParam final KlantListParameters listParameters
    ) throws ProcessingException;

    /**
     * Werk een KLANT deels bij.
     */
    @PATCH
    @Path("/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    Klant klantPartialUpdate(
            @PathParam("uuid") UUID uuid,
            @HeaderParam("Content-Type") String contentType,
            Klant klant,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;

    /**
     * Een specifiek KLANT opvragen.
     */
    @GET
    @Path("/{uuid}")
    @Produces({"application/json", "application/problem+json"})
    Klant klantRead(
            @PathParam("uuid") UUID uuid,
            @HeaderParam("If-None-Match") String ifNoneMatch
    ) throws ProcessingException;

    /**
     * Werk een KLANT in zijn geheel bij.
     */
    @PUT
    @Path("/{uuid}")
    @Consumes({"application/json"})
    @Produces({"application/json", "application/problem+json"})
    Klant klantUpdate(
            @PathParam("uuid") UUID uuid,
            @HeaderParam("Content-Type") String contentType,
            Klant klant,
            @HeaderParam(X_NLX_LOGRECORD_ID_HEADER) String xNLXLogrecordID,
            @HeaderParam(X_AUDIT_TOELICHTING_HEADER) String xAuditToelichting
    ) throws ProcessingException;
}
