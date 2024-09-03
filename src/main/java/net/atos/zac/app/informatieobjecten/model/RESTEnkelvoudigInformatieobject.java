/*
 * SPDX-FileCopyrightText: 2021 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.informatieobjecten.model;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.EnumSet;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.FormParam;

import net.atos.client.zgw.drc.model.generated.StatusEnum;
import net.atos.zac.app.identity.model.RestUser;
import net.atos.zac.app.policy.model.RestDocumentRechten;
import net.atos.zac.zoeken.model.DocumentIndicatie;

/**
 * Representation of an 'enkelvoudig informatieobject' (e.g. a document) in the ZAC REST API.
 */
public class RESTEnkelvoudigInformatieobject extends RESTEnkelvoudigInformatieFileUpload {

    public UUID uuid;

    @FormParam("identificatie")
    public String identificatie;

    @NotNull @FormParam("titel")
    public String titel;

    @FormParam("beschrijving")
    public String beschrijving;

    // not always required
    @FormParam("creatiedatum")
    public LocalDate creatiedatum;

    @FormParam("registratiedatumTijd")
    public ZonedDateTime registratiedatumTijd;

    @FormParam("ontvangstdatum")
    public LocalDate ontvangstdatum;

    @FormParam("verzenddatum")
    public LocalDate verzenddatum;

    @FormParam("bronorganisatie")
    public String bronorganisatie;

    // not always required
    @FormParam("vertrouwelijkheidaanduiding")
    public String vertrouwelijkheidaanduiding;

    // not always required
    @FormParam("auteur")
    public String auteur;

    @FormParam("status")
    public StatusEnum status;

    @FormParam("formaat")
    public String formaat;

    @FormParam("bestandsomvang")
    public Long bestandsomvang;

    // not always required
    @FormParam("taal")
    public String taal;

    @FormParam("versie")
    public Integer versie;

    @NotNull @FormParam("informatieobjectTypeUUID")
    public UUID informatieobjectTypeUUID;

    @FormParam("informatieobjectTypeOmschrijving")
    public String informatieobjectTypeOmschrijving;

    @FormParam("link")
    public String link;

    @FormParam("ondertekening")
    public RESTOndertekening ondertekening;

    @FormParam("indicatieGebruiksrecht")
    public boolean indicatieGebruiksrecht;

    public EnumSet<DocumentIndicatie> getIndicaties() {
        final EnumSet<DocumentIndicatie> indicaties = EnumSet.noneOf(DocumentIndicatie.class);
        if (gelockedDoor != null) {
            indicaties.add(DocumentIndicatie.VERGRENDELD);
        }
        if (ondertekening != null) {
            indicaties.add(DocumentIndicatie.ONDERTEKEND);
        }
        if (indicatieGebruiksrecht) {
            indicaties.add(DocumentIndicatie.GEBRUIKSRECHT);
        }
        if (isBesluitDocument) {
            indicaties.add(DocumentIndicatie.BESLUIT);
        }
        if (verzenddatum != null) {
            indicaties.add(DocumentIndicatie.VERZONDEN);
        }
        return indicaties;
    }

    @FormParam("gelockedDoor")
    public RestUser gelockedDoor;

    @FormParam("isBesluitDocument")
    public boolean isBesluitDocument;

    @FormParam("rechten")
    public RestDocumentRechten rechten;
}
