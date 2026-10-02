/*
 * SPDX-FileCopyrightText: 2022 Atos, 2025 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.policy.output

import jakarta.json.bind.annotation.JsonbCreator
import jakarta.json.bind.annotation.JsonbProperty
import nl.info.client.opa.model.OpaRuleResult

data class ZaakRechten @JsonbCreator constructor(
    @param:JsonbProperty("lezen") val canLezen: Boolean,
    @param:JsonbProperty("wijzigen") val canWijzigen: Boolean,
    @param:JsonbProperty("toekennen") val canToekennen: Boolean,
    @param:JsonbProperty("behandelen") val canBehandelen: Boolean,
    @param:JsonbProperty("afbreken") val canAfbreken: Boolean,
    @param:JsonbProperty("heropenen") val canHeropenen: Boolean,
    @param:JsonbProperty("bekijken_zaakdata") val canBekijkenZaakdata: Boolean,
    @param:JsonbProperty("wijzigen_doorlooptijd") val canWijzigenDoorlooptijd: Boolean,
    @param:JsonbProperty("verlengen") val canVerlengen: Boolean,
    @param:JsonbProperty("opschorten") val canOpschorten: Boolean,
    @param:JsonbProperty("hervatten") val canHervatten: Boolean,
    @param:JsonbProperty("creeren_document") val canCreerenDocument: Boolean,
    @param:JsonbProperty("toevoegen_document") val canToevoegenDocument: Boolean,
    @param:JsonbProperty("koppelen") val canKoppelen: Boolean,
    @param:JsonbProperty("versturen_email") val canVersturenEmail: Boolean,
    @param:JsonbProperty("versturen_ontvangstbevestiging") val canVersturenOntvangstbevestiging: Boolean,
    @param:JsonbProperty("toevoegen_initiator_persoon") val canToevoegenInitiatorPersoon: Boolean,
    @param:JsonbProperty("toevoegen_initiator_bedrijf") val canToevoegenInitiatorBedrijf: Boolean,
    @param:JsonbProperty("verwijderen_initiator") val canVerwijderenInitiator: Boolean,
    @param:JsonbProperty("toevoegen_betrokkene_persoon") val canToevoegenBetrokkenePersoon: Boolean,
    @param:JsonbProperty("toevoegen_betrokkene_bedrijf") val canToevoegenBetrokkeneBedrijf: Boolean,
    @param:JsonbProperty("verwijderen_betrokkene") val canVerwijderenBetrokkene: Boolean,
    @param:JsonbProperty("toevoegen_bag_object") val canToevoegenBagObject: Boolean,
    @param:JsonbProperty("starten_taak") val canStartenTaak: Boolean,
    @param:JsonbProperty("vastleggen_besluit") val canVastleggenBesluit: Boolean,
    @param:JsonbProperty("verlengen_doorlooptijd") val canVerlengenDoorlooptijd: Boolean,
    @param:JsonbProperty("wijzigen_locatie") val canWijzigenLocatie: Boolean,
    @param:JsonbProperty("brondatum_zetten") val canBrondatumZetten: Boolean
) : OpaRuleResult
