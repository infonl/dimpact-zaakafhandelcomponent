package net.atos.zac.app.documentcreation.converter

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.inject.Singleton
import net.atos.client.zgw.drc.decodedBase64StringLength
import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObjectCreateLockRequest
import net.atos.client.zgw.drc.model.generated.StatusEnum
import net.atos.client.zgw.drc.model.generated.VertrouwelijkheidaanduidingEnum
import net.atos.client.zgw.zrc.model.Zaak
import net.atos.client.zgw.ztc.ZtcClientService
import net.atos.zac.app.documentcreation.model.RestDocumentCreationUnattendedData
import net.atos.zac.authentication.LoggedInUser
import net.atos.zac.configuratie.ConfiguratieService
import net.atos.zac.documentcreation.model.DocumentCreationUnattendedResponse
import net.atos.zac.identity.model.getFullName
import net.atos.zac.smartdocuments.SmartDocumentsTemplatesService
import net.atos.zac.util.UriUtil.uuidFromURI
import nl.lifely.zac.util.AllOpen
import nl.lifely.zac.util.NoArgConstructor
import java.time.LocalDate

@Singleton
@NoArgConstructor
@AllOpen
class RestDocumentCreationConverter @Inject constructor(
    private val loggedInUserInstance: Instance<LoggedInUser>,
    private val ztcClientService: ZtcClientService,
    private val smartDocumentsTemplatesService: SmartDocumentsTemplatesService,
) {

    fun toEnkelvoudigInformatieObjectCreateLockRequest(
        zaak: Zaak,
        unattendedData: RestDocumentCreationUnattendedData,
        unattendedResponse: DocumentCreationUnattendedResponse
    ) = EnkelvoudigInformatieObjectCreateLockRequest().apply {
        bronorganisatie = ConfiguratieService.BRON_ORGANISATIE
        creatiedatum = unattendedData.creationDate ?: LocalDate.now()
        titel = unattendedData.documentTitle ?: unattendedResponse.fileName
        auteur = unattendedData.author ?: loggedInUserInstance.get().getFullName()
        taal = ConfiguratieService.TAAL_NEDERLANDS
        beschrijving = ConfiguratieService.OMSCHRIJVING_TAAK_DOCUMENT
        status = StatusEnum.DEFINITIEF
        vertrouwelijkheidaanduiding = VertrouwelijkheidaanduidingEnum.OPENBAAR
        informatieobjecttype = smartDocumentsTemplatesService.getInformationObjectTypeUUID(
            zaakafhandelParametersUUID = uuidFromURI(zaak.zaaktype),
            templateGroupName = unattendedData.smartDocumentsTemplateGroupName,
            templateName = unattendedData.smartDocumentsTemplateName
        ).let {
            ztcClientService.readInformatieobjecttype(it).url
        }
        bestandsnaam = unattendedResponse.fileName
        formaat = unattendedResponse.fileType
        inhoud = unattendedResponse.fileContent
        bestandsomvang = unattendedResponse.fileContent?.decodedBase64StringLength()
    }

}
