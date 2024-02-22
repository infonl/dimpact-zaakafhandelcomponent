/*
 * SPDX-FileCopyrightText: 2022 Atos, 2023-2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */
package net.atos.zac.app.zoeken.converter;

import java.util.EnumMap;
import java.util.List;

import jakarta.inject.Inject;

import net.atos.client.zgw.ztc.model.generated.RolType;
import net.atos.zac.app.policy.converter.RESTRechtenConverter;
import net.atos.zac.app.zoeken.model.RESTZaakZoekObject;
import net.atos.zac.policy.PolicyService;
import net.atos.zac.util.DateTimeConverterUtil;
import net.atos.zac.zoeken.model.ZaakIndicatie;
import net.atos.zac.zoeken.model.zoekobject.ZaakZoekObject;

public class RESTZaakZoekObjectConverter {

    @Inject private PolicyService policyService;

    @Inject private RESTRechtenConverter restRechtenConverter;

    public RESTZaakZoekObject convert(final ZaakZoekObject zoekItem) {
        final RESTZaakZoekObject restZoekItem = new RESTZaakZoekObject();
        restZoekItem.id = zoekItem.getUuid();
        restZoekItem.type = zoekItem.getType();
        restZoekItem.identificatie = zoekItem.getIdentificatie();
        restZoekItem.omschrijving = zoekItem.getOmschrijving();
        restZoekItem.toelichting = zoekItem.getToelichting();
        restZoekItem.archiefNominatie = zoekItem.getArchiefNominatie();
        restZoekItem.archiefActiedatum =
                DateTimeConverterUtil.convertToLocalDate(zoekItem.getArchiefActiedatum());
        restZoekItem.registratiedatum =
                DateTimeConverterUtil.convertToLocalDate(zoekItem.getRegistratiedatum());
        restZoekItem.startdatum =
                DateTimeConverterUtil.convertToLocalDate(zoekItem.getStartdatum());
        restZoekItem.einddatum = DateTimeConverterUtil.convertToLocalDate(zoekItem.getEinddatum());
        restZoekItem.einddatumGepland =
                DateTimeConverterUtil.convertToLocalDate(zoekItem.getEinddatumGepland());
        restZoekItem.uiterlijkeEinddatumAfdoening =
                DateTimeConverterUtil.convertToLocalDate(
                        zoekItem.getUiterlijkeEinddatumAfdoening());
        restZoekItem.publicatiedatum =
                DateTimeConverterUtil.convertToLocalDate(zoekItem.getPublicatiedatum());
        restZoekItem.communicatiekanaal = zoekItem.getCommunicatiekanaal();
        restZoekItem.vertrouwelijkheidaanduiding = zoekItem.getVertrouwelijkheidaanduiding();
        restZoekItem.afgehandeld = zoekItem.isAfgehandeld();
        restZoekItem.groepId = zoekItem.getGroepID();
        restZoekItem.groepNaam = zoekItem.getGroepNaam();
        restZoekItem.behandelaarNaam = zoekItem.getBehandelaarNaam();
        restZoekItem.behandelaarGebruikersnaam = zoekItem.getBehandelaarGebruikersnaam();
        restZoekItem.initiatorIdentificatie = zoekItem.getInitiatorIdentificatie();
        restZoekItem.zaaktypeOmschrijving = zoekItem.getZaaktypeOmschrijving();
        restZoekItem.statustypeOmschrijving = zoekItem.getStatustypeOmschrijving();
        restZoekItem.resultaattypeOmschrijving = zoekItem.getResultaattypeOmschrijving();
        restZoekItem.aantalOpenstaandeTaken = zoekItem.getAantalOpenstaandeTaken();
        restZoekItem.indicatieVerlenging = zoekItem.isIndicatie(ZaakIndicatie.VERLENGD);
        restZoekItem.redenVerlenging = zoekItem.getRedenVerlenging();
        restZoekItem.indicatieOpschorting = zoekItem.isIndicatie(ZaakIndicatie.OPSCHORTING);
        restZoekItem.redenOpschorting = zoekItem.getRedenOpschorting();
        restZoekItem.indicatieDeelzaak = zoekItem.isIndicatie(ZaakIndicatie.DEELZAAK);
        restZoekItem.indicatieHoofdzaak = zoekItem.isIndicatie(ZaakIndicatie.HOOFDZAAK);
        restZoekItem.indicatieHeropend = zoekItem.isIndicatie(ZaakIndicatie.HEROPEND);
        restZoekItem.statusToelichting = zoekItem.getStatusToelichting();
        restZoekItem.indicaties = zoekItem.getZaakIndicaties();
        restZoekItem.rechten =
                restRechtenConverter.convert(policyService.readZaakRechten(zoekItem));
        restZoekItem.betrokkenen = new EnumMap<>(RolType.OmschrijvingGeneriekEnum.class);
        if (zoekItem.getInitiatorIdentificatie() != null) {
            restZoekItem.betrokkenen.put(
                    RolType.OmschrijvingGeneriekEnum.INITIATOR,
                    List.of(zoekItem.getInitiatorIdentificatie()));
        }
        if (zoekItem.getBetrokkenen() != null) {
            zoekItem.getBetrokkenen()
                    .forEach(
                            (betrokkenheid, ids) -> {
                                restZoekItem.betrokkenen.put(
                                        RolType.OmschrijvingGeneriekEnum.valueOf(
                                                betrokkenheid
                                                        .replace(
                                                                ZaakZoekObject
                                                                        .ZAAK_BETROKKENE_PREFIX,
                                                                "")
                                                        .toUpperCase()),
                                        ids);
                            });
        }
        return restZoekItem;
    }
}
