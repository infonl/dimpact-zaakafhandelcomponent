/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import net.atos.zac.util.SerializableByYasson
import nl.info.zac.util.AllOpen
import nl.info.zac.util.NoArgConstructor

@NoArgConstructor
@AllOpen
data class ZaakRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val lezen: Boolean,
    @param:JsonbProperty("wijzigen") val wijzigen: Boolean,
    @param:JsonbProperty("toekennen") val toekennen: Boolean,
    @param:JsonbProperty("behandelen") val behandelen: Boolean,
    @param:JsonbProperty("afbreken") val afbreken: Boolean,
    @param:JsonbProperty("heropenen") val heropenen: Boolean,
    @param:JsonbProperty("bekijken_zaakdata") val bekijkenZaakdata: Boolean,
    @param:JsonbProperty("wijzigen_doorlooptijd") val wijzigenDoorlooptijd: Boolean,
    @param:JsonbProperty("verlengen") val verlengen: Boolean,
    @param:JsonbProperty("opschorten") val opschorten: Boolean,
    @param:JsonbProperty("hervatten") val hervatten: Boolean,
    @param:JsonbProperty("creeeren_document") val creeerenDocument: Boolean,
    @param:JsonbProperty("toevoegen_document") val toevoegenDocument: Boolean,
    @param:JsonbProperty("koppelen") val koppelen: Boolean,
    @param:JsonbProperty("versturen_email") val versturenEmail: Boolean,
    @param:JsonbProperty("versturen_ontvangstbevestiging") val versturenOntvangstbevestiging: Boolean,
    @param:JsonbProperty("toevoegen_initiator_persoon") val toevoegenInitiatorPersoon: Boolean,
    @param:JsonbProperty("toevoegen_initiator_bedrijf") val toevoegenInitiatorBedrijf: Boolean,
    @param:JsonbProperty("verwijderen_initiator") val verwijderenInitiator: Boolean,
    @param:JsonbProperty("toevoegen_betrokkene_persoon") val toevoegenBetrokkenePersoon: Boolean,
    @param:JsonbProperty("toevoegen_betrokkene_bedrijf") val toevoegenBetrokkeneBedrijf: Boolean,
    @param:JsonbProperty("verwijderen_betrokkene") val verwijderenBetrokkene: Boolean,
    @param:JsonbProperty("toevoegen_bag_object") val toevoegenBagObject: Boolean,
    @param:JsonbProperty("starten_taak") val startenTaak: Boolean,
    @param:JsonbProperty("vastleggen_besluit") val vastleggenBesluit: Boolean,
    @param:JsonbProperty("verlengen_doorlooptijd") val verlengenDoorlooptijd: Boolean,
    @param:JsonbProperty("wijzigen_locatie") val wijzigenLocatie: Boolean
) : SerializableByYasson
