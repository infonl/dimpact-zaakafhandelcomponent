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

import net.atos.client.zgw.drc.model.generated.EnkelvoudigInformatieObject;
import net.atos.zac.app.identity.model.RESTUser;
import net.atos.zac.app.policy.model.RESTDocumentRechten;
import net.atos.zac.zoeken.model.DocumentIndicatie;

/**
 * Representation of an 'enkelvoudig informatieobject' (e.g. a document) in the ZAC REST API.
 */
public class RESTEnkelvoudigInformatieobject {


    public UUID uuid;

    public String identificatie;

    @NotNull
    public String titel;

    public String beschrijving;

    @NotNull
    public LocalDate creatiedatum;

    public ZonedDateTime registratiedatumTijd;

    public LocalDate ontvangstdatum;

    public LocalDate verzenddatum;

    public String bronorganisatie;

    @NotNull
    public String vertrouwelijkheidaanduiding;

    @NotNull
    public String auteur;

    public EnkelvoudigInformatieObject.StatusEnum status;

    public String formaat;

    @NotNull
    public String taal;

    public Integer versie;

    @NotNull
    public UUID informatieobjectTypeUUID;


    public String informatieobjectTypeOmschrijving;

    @NotNull
    public String bestandsnaam;

    public Long bestandsomvang;

    public String link;

    public RESTOndertekening ondertekening;

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

    public RESTUser gelockedDoor;

    public boolean isBesluitDocument;

    public RESTDocumentRechten rechten;
}
