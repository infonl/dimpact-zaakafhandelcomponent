package net.atos.zac.app.informatieobjecten.model;

import jakarta.ws.rs.FormParam;

import net.atos.zac.app.informatieobjecten.model.validation.ValidRestEnkelvoudigInformatieFileUploadForm;

@ValidRestEnkelvoudigInformatieFileUploadForm
public abstract class RESTEnkelvoudigInformatieFileUpload {

    @FormParam("file")
    public byte[] file;

    @FormParam("bestandsnaam")
    public String bestandsnaam;

}
