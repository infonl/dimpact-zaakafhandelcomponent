/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.policy.output;

import jakarta.json.bind.annotation.JsonbCreator;
import jakarta.json.bind.annotation.JsonbProperty;

import net.atos.zac.util.SerializableByYasson;

public record ZaakRechten(
                          boolean lezen,
                          boolean wijzigen,
                          boolean toekennen,
                          boolean behandelen,
                          boolean afbreken,
                          boolean heropenen,
                          boolean bekijkenZaakdata,
                          boolean wijzigenDoorlooptijd,
                          boolean verlengen,
                          boolean opschorten,
                          boolean hervatten,
                          boolean creeerenDocument,
                          boolean toevoegenDocument,
                          boolean koppelen,
                          boolean versturenEmail,
                          boolean versturenOntvangstbevestiging,
                          boolean toevoegenInitiatorPersoon,
                          boolean toevoegenInitiatorBedrijf,
                          boolean verwijderenInitiator,
                          boolean toevoegenBetrokkenePersoon,
                          boolean toevoegenBetrokkeneBedrijf,
                          boolean verwijderenBetrokkene,
                          boolean toevoegenBagObject,
                          boolean startenTaak,
                          boolean vastleggenBesluit,
                          boolean verlengenDoorlooptijd,
                          boolean wijzigenLocatie
) implements SerializableByYasson {

    @JsonbCreator
    public ZaakRechten(
            @JsonbProperty("lezen") final boolean lezen,
            @JsonbProperty("wijzigen") final boolean wijzigen,
            @JsonbProperty("toekennen") final boolean toekennen,
            @JsonbProperty("behandelen") final boolean behandelen,
            @JsonbProperty("afbreken") final boolean afbreken,
            @JsonbProperty("heropenen") final boolean heropenen,
            @JsonbProperty("bekijken_zaakdata") final boolean bekijkenZaakdata,
            @JsonbProperty("wijzigen_doorlooptijd") final boolean wijzigenDoorlooptijd,
            @JsonbProperty("verlengen") final boolean verlengen,
            @JsonbProperty("opschorten") final boolean opschorten,
            @JsonbProperty("hervatten") final boolean hervatten,
            @JsonbProperty("creeeren_document") final boolean creeerenDocument,
            @JsonbProperty("toevoegen_document") final boolean toevoegenDocument,
            @JsonbProperty("koppelen") final boolean koppelen,
            @JsonbProperty("versturen_email") final boolean versturenEmail,
            @JsonbProperty("versturen_ontvangstbevestiging") final boolean versturenOntvangstbevestiging,
            @JsonbProperty("toevoegen_initiator_persoon") final boolean toevoegenInitiatorPersoon,
            @JsonbProperty("toevoegen_initiator_bedrijf") final boolean toevoegenInitiatorBedrijf,
            @JsonbProperty("verwijderen_initiator") final boolean verwijderenInitiator,
            @JsonbProperty("toevoegen_betrokkene_persoon") final boolean toevoegenBetrokkenePersoon,
            @JsonbProperty("toevoegen_betrokkene_bedrijf") final boolean toevoegenBetrokkeneBedrijf,
            @JsonbProperty("verwijderen_betrokkene") final boolean verwijderenBetrokkene,
            @JsonbProperty("toevoegen_bag_object") final boolean toevoegenBagObject,
            @JsonbProperty("starten_taak") final boolean startenTaak,
            @JsonbProperty("vastleggen_besluit") final boolean vastleggenBesluit,
            @JsonbProperty("verlengen_doorlooptijd") final boolean verlengenDoorlooptijd,
            @JsonbProperty("wijzigen_locatie") final boolean wijzigenLocatie
    ) {
        this.lezen = lezen;
        this.wijzigen = wijzigen;
        this.toekennen = toekennen;
        this.behandelen = behandelen;
        this.afbreken = afbreken;
        this.heropenen = heropenen;
        this.bekijkenZaakdata = bekijkenZaakdata;
        this.wijzigenDoorlooptijd = wijzigenDoorlooptijd;
        this.verlengen = verlengen;
        this.opschorten = opschorten;
        this.hervatten = hervatten;
        this.creeerenDocument = creeerenDocument;
        this.toevoegenDocument = toevoegenDocument;
        this.koppelen = koppelen;
        this.versturenEmail = versturenEmail;
        this.versturenOntvangstbevestiging = versturenOntvangstbevestiging;
        this.toevoegenInitiatorPersoon = toevoegenInitiatorPersoon;
        this.toevoegenInitiatorBedrijf = toevoegenInitiatorBedrijf;
        this.verwijderenInitiator = verwijderenInitiator;
        this.toevoegenBetrokkenePersoon = toevoegenBetrokkenePersoon;
        this.toevoegenBetrokkeneBedrijf = toevoegenBetrokkeneBedrijf;
        this.verwijderenBetrokkene = verwijderenBetrokkene;
        this.toevoegenBagObject = toevoegenBagObject;
        this.startenTaak = startenTaak;
        this.vastleggenBesluit = vastleggenBesluit;
        this.verlengenDoorlooptijd = verlengenDoorlooptijd;
        this.wijzigenLocatie = wijzigenLocatie;
    }
}
