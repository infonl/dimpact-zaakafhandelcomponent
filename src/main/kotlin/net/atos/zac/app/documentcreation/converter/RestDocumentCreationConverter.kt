package net.atos.zac.app.documentcreation.converter

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.zac.app.documentcreation.model.RestDocumentCreationUnattendedData
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.model.DocumentCreationUnattendedResponse

fun RestDocumentCreationUnattendedData.toEnkelvoudigInformatieObjectCreateLockRequest(
    unattendedResponse: DocumentCreationUnattendedResponse
) = EnkelvoudigInformatieObjectCreateLockRequest().apply {
    bronorganisatie = ConfiguratieService.BRON_ORGANISATIE
    creatiedatum = creationDate
    titel = documentTitle
    auteur = author
    taal = ConfiguratieService.TAAL_NEDERLANDS
    beschrijving = ConfiguratieService.OMSCHRIJVING_TAAK_DOCUMENT
    status = StatusEnum.DEFINITIEF
    vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR

//    informatieobjecttype = ztcClientService.readInformatieobjecttype(restEnkelvoudigInformatieobject.informatieobjectTypeUUID).getUrl()

    bestandsnaam = unattendedResponse.fileName
    formaat = unattendedResponse.fileType
    inhoud = unattendedResponse.fileContent
    bestandsomvang = unattendedResponse.fileContent?.length
}
