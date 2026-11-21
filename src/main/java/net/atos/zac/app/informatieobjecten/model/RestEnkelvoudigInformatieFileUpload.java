/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.informatieobjecten.model;

import jakarta.ws.rs.FormParam;

import net.atos.zac.app.informatieobjecten.model.validation.ValidRestEnkelvoudigInformatieFileUploadForm;

@ValidRestEnkelvoudigInformatieFileUploadForm
public abstract class RestEnkelvoudigInformatieFileUpload {

    // this can be empty when adding a new version in which only the metadata changes
    @FormParam("file")
    public byte[] file;

    @FormParam("bestandsnaam")
    public String bestandsnaam;
}
