/*
 * SPDX-FileCopyrightText: 2021 - 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementatie van ExceptionMapper. Alle exceptions worden gecatched door de JAX-RS runtime en gemapped naar een {@link Response}.
 */
@Provider
public class RESTExceptionMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(RESTExceptionMapper.class.getName());

    /**
     * Retourneert een {@link Response} naar de Angular client.
     */
    @Override
    public Response toResponse(final Exception e) {
        if (e instanceof WebApplicationException &&
            Response.Status.Family.familyOf(((WebApplicationException) e).getResponse().getStatus()) !=
                                                    Response.Status.Family.SERVER_ERROR) {
            final WebApplicationException wae = (WebApplicationException) e;
            return Response.status(wae.getResponse().getStatus())
                    .type(MediaType.APPLICATION_JSON)
                    .entity(getJSONMessage(e, wae.getMessage()))
                    .build();
        } else {
            LOG.log(Level.SEVERE, e.getMessage(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .type(MediaType.APPLICATION_JSON)
                    .entity(getJSONMessage(e, "Algemene Fout"))
                    .build();
        }
    }

    private static String getJSONMessage(final Exception e, final String melding) {
        final Map<String, Object> data = new HashMap<>();
        data.put("message", melding);
        data.put("exception", e.getMessage());
        data.put("stackTrace", ExceptionUtils.getStackTrace(e));
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(data);
        } catch (final IOException ioe) {
            //Het omzetten van de exceptie naar een JSON bericht is fout gegaan.
            LOG.severe(ioe.getMessage());
        }
        return null;
    }
}
