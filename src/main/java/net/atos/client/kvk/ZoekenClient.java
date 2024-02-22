/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.kvk;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.annotation.RegisterClientHeaders;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import net.atos.client.kvk.exception.KvKClientNoResultExceptionMapper;
import net.atos.client.kvk.exception.RuntimeExceptionMapper;
import net.atos.client.kvk.model.KVKZoekenParameters;
import net.atos.client.kvk.util.KVKClientHeadersFactory;
import net.atos.client.kvk.zoeken.model.Resultaat;

@RegisterRestClient(configKey = "KVK-API-Client")
@RegisterClientHeaders(KVKClientHeadersFactory.class)
@RegisterProvider(RuntimeExceptionMapper.class)
@RegisterProvider(KvKClientNoResultExceptionMapper.class)
@Produces({"application/hal+json"})
@Path("api/v1/zoeken")
@Timeout(unit = ChronoUnit.SECONDS, value = 5)
public interface ZoekenClient {

  /**
   * Voor een bedrijf zoeken naar basisinformatie.
   * <p>
   * Er wordt max. 1000 resultaten getoond.
   */
  @GET
  Resultaat getResults(@BeanParam final KVKZoekenParameters zoekenParameters);

  /**
   * Voor een bedrijf zoeken naar basisinformatie asynchron.
   * <p>
   * Er wordt max. 1000 resultaten getoond.
   */
  @GET
  CompletionStage<Resultaat> getResultsAsync(@BeanParam final KVKZoekenParameters zoekenParameters);
}
