package net.atos.zac.app.informatieobjecten.model;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;
import net.atos.zac.app.informatieobjecten.model.validation.ValidRestEnkelvoudigInformatieFileUploadForm;

@ValidRestEnkelvoudigInformatieFileUploadForm
public abstract class RESTEnkelvoudigInformatieFileUpload {

  @FormParam("file")
  public byte[] file;

  @NotNull
  @FormParam("bestandsnaam")
  public String bestandsnaam;

}
