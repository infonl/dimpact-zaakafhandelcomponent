/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.app.zoeken.converter;

import jakarta.inject.Inject;

import net.atos.zac.app.policy.converter.RestRechtenConverter;
import net.atos.zac.app.zoeken.model.RESTTaakZoekObject;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.util.time.DateTimeConverterUtil;
import net.atos.zac.zoeken.model.zoekobject.TaakZoekObject;

public class RESTTaakZoekObjectConverter {

    @Inject
    private PolicyService policyService;

    public RESTTaakZoekObject convert(final TaakZoekObject taakZoekObject) {
        final RESTTaakZoekObject restTaakZoekObject = new RESTTaakZoekObject();
        restTaakZoekObject.id = taakZoekObject.getObjectId();
        restTaakZoekObject.type = taakZoekObject.getType();
        restTaakZoekObject.naam = taakZoekObject.getNaam();
        restTaakZoekObject.status = taakZoekObject.getStatus();
        restTaakZoekObject.toelichting = taakZoekObject.getToelichting();
        restTaakZoekObject.creatiedatum = DateTimeConverterUtil.convertToLocalDate(taakZoekObject.getCreatiedatum());
        restTaakZoekObject.toekenningsdatum = DateTimeConverterUtil.convertToLocalDate(taakZoekObject.getToekenningsdatum());
        restTaakZoekObject.fataledatum = DateTimeConverterUtil.convertToLocalDate(taakZoekObject.getFataledatum());
        restTaakZoekObject.groepNaam = taakZoekObject.getGroepNaam();
        restTaakZoekObject.behandelaarNaam = taakZoekObject.getBehandelaarNaam();
        restTaakZoekObject.behandelaarGebruikersnaam = taakZoekObject.getBehandelaarGebruikersnaam();
        restTaakZoekObject.zaaktypeOmschrijving = taakZoekObject.getZaaktypeOmschrijving();
        restTaakZoekObject.zaakIdentificatie = taakZoekObject.getZaakIdentificatie();
        restTaakZoekObject.zaakUuid = taakZoekObject.getZaakUUID();
        restTaakZoekObject.zaakToelichting = taakZoekObject.getZaakToelichting();
        restTaakZoekObject.zaakOmschrijving = taakZoekObject.getZaakOmschrijving();
        restTaakZoekObject.rechten = RestRechtenConverter.convert(policyService.readTaakRechten(taakZoekObject));
        return restTaakZoekObject;
    }
}
