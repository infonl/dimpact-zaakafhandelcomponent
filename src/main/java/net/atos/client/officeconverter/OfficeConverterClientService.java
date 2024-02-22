/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.client.officeconverter;

import java.io.ByteArrayInputStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataOutput;

@ApplicationScoped
public class OfficeConverterClientService {

  @Inject @RestClient private OfficeConverterClient officeConverterClient;

  public ByteArrayInputStream convertToPDF(
      final ByteArrayInputStream document, final String filename) {
    final MultipartFormDataOutput multipartFormDataOutput = new MultipartFormDataOutput();
    multipartFormDataOutput.addFormData(
        "file", document, MediaType.APPLICATION_OCTET_STREAM_TYPE, filename);
    final Response response = officeConverterClient.convert(multipartFormDataOutput);
    if (!response.bufferEntity()) {
      throw new RuntimeException("Content of PDF converter could not be buffered.");
    }
    return (ByteArrayInputStream) response.getEntity();
  }
}
