/*
 * SPDX-FileCopyrightText: 2022 Atos, 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.client.kvk;

import java.time.temporal.ChronoUnit;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.kvk.exception.KvkClientNoResultResponseExceptionMapper;
import net.atos.client.kvk.exception.KvkRuntimeExceptionMapper;
import net.atos.client.kvk.model.KvkZoekenParameters;
import net.atos.client.kvk.util.KvkClientHeadersFactory;
import net.atos.client.kvk.zoeken.model.generated.Resultaat;
import net.atos.zac.util.MediaTypes;

@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KvkClientHeadersFactory.class)
@RegisterProvider(KvkRuntimeExceptionMapper.class)
@RegisterProvider(KvkClientNoResultResponseExceptionMapper.class)
@Produces({MediaTypes.MEDIA_TYPE_HAL_JSON})
@Path("api/v2/zoeken")
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
public interface ZoekenClient {

    /**
     * Voor een bedrijf zoeken naar basisinformatie.
     * <p>
     * Er wordt max. 1000 resultaten getoond.
     */
    @GET
    Resultaat getResults(@BeanParam final KvkZoekenParameters zoekenParameters);
}
