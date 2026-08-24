#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.zaak

import rego.v1

import data.net.atos.zac.zaak.zaaktype_allowed
import data.net.atos.zac.zaak.zaak_allowed
import data.net.atos.zac.zaak.lezen
import data.net.atos.zac.zaak.wijzigen
import data.net.atos.zac.zaak.toekennen
import data.net.atos.zac.zaak.behandelen
import data.net.atos.zac.zaak.afbreken
import data.net.atos.zac.zaak.heropenen
import data.net.atos.zac.zaak.bekijken_zaakdata
import data.net.atos.zac.zaak.wijzigen_doorlooptijd
import data.net.atos.zac.zaak.verlengen
import data.net.atos.zac.zaak.opschorten
import data.net.atos.zac.zaak.hervatten
import data.net.atos.zac.zaak.creeren_document
import data.net.atos.zac.zaak.toevoegen_document
import data.net.atos.zac.zaak.koppelen
import data.net.atos.zac.zaak.koppelen_gerelateerd
import data.net.atos.zac.zaak.versturen_email
import data.net.atos.zac.zaak.versturen_ontvangstbevestiging
import data.net.atos.zac.zaak.toevoegen_initiator_persoon
import data.net.atos.zac.zaak.toevoegen_initiator_bedrijf
import data.net.atos.zac.zaak.verwijderen_initiator
import data.net.atos.zac.zaak.toevoegen_betrokkene_persoon
import data.net.atos.zac.zaak.toevoegen_betrokkene_bedrijf
import data.net.atos.zac.zaak.verwijderen_betrokkene
import data.net.atos.zac.zaak.toevoegen_bag_object
import data.net.atos.zac.zaak.starten_taak
import data.net.atos.zac.zaak.vastleggen_besluit
import data.net.atos.zac.zaak.verlengen_doorlooptijd
import data.net.atos.zac.zaak.wijzigen_locatie
import data.net.atos.zac.zaak.brondatum_zetten


##################
# zaaktype_allowed
##################
test_zaaktype_allowed if {
    zaaktype_allowed
        with input.zaak.zaaktype as "type"
        with input.user.zaaktypen as ["first", "type"]
}

test_zaaktype_allowed_missing_user_zaaktypen if {
    zaaktype_allowed
        with input.zaak.zaaktype as "type"
}

test_zaaktype_allowed_zaak_zaaktype_not_in_user_zaaktypen_fails if {
    not zaaktype_allowed
        with input.zaak.zaaktype as "missing"
        with input.user.zaaktypen as ["first", "type"]
}

#######
# lezen
#######
test_lezen if {
    lezen with input.user.rollen as [ "raadpleger" ]
}

test_lezen_with_behandelaar_role if {
    lezen with input.user.rollen as [ "behandelaar" ]
}

test_lezen_with_coordinator_role if {
    lezen with input.user.rollen as [ "coordinator" ]
}

test_lezen_with_recordmanager_role if {
    lezen with input.user.rollen as [ "recordmanager" ]
}

test_lezen_with_beheerder_role if {
    lezen with input.user.rollen as [ "beheerder" ]
}

test_lezen_wrong_role_fails if {
    not lezen with input.user.rollen as [ "fakeRole" ]
}

test_lezen_missing_role_fails if {
    not lezen with input.user.key as "value"
}

##########
# wijzigen
##########
test_wijzigen_behandelaar if {
    wijzigen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_wijzigen_behandelaar_zaak_closed_fails if {
    not wijzigen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_wijzigen_coordinator if {
    wijzigen
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_wijzigen_coordinator_zaak_closed_fails if {
    not wijzigen
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_wijzigen_recordmanager if {
    wijzigen
        with input.user.rollen as [ "recordmanager" ]
}

test_wijzigen_beheerder if {
    wijzigen
        with input.user.rollen as [ "beheerder" ]
}

test_wijzigen_beheerder_zaak_closed_still_succeeds if {
    wijzigen
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as false
}

test_wijzigen_wrong_role_fails if {
    not wijzigen with input.user.rollen as [ "fakeRole" ]
}

test_wijzigen_raadpleger_role_fails if {
    not wijzigen
        with input.user.rollen as [ "raadpleger" ]
        with input.zaak.open as true
}

test_wijzigen_missing_role_fails if {
    not wijzigen with input.user.key as "value"
}

###########
# toekennen
###########
test_toekennen_behandelaar if {
    toekennen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_toekennen_behandelaar_zaak_closed_fails if {
    not toekennen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_toekennen_coordinator if {
    toekennen
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_toekennen_coordinator_zaak_closed_fails if {
    not toekennen
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_toekennen_recordmanager if {
    toekennen
        with input.user.rollen as [ "recordmanager" ]
}

test_toekennen_beheerder if {
    toekennen
        with input.user.rollen as [ "beheerder" ]
}

test_toekennen_wrong_role_fails if {
    not toekennen with input.user.rollen as [ "fakeRole" ]
}

test_toekennen_raadpleger_role_fails if {
    not toekennen
        with input.user.rollen as [ "raadpleger" ]
        with input.zaak.open as true
}

test_toekennen_missing_role_fails if {
    not toekennen with input.user.key as "value"
}

############
# behandelen
############
test_behandelen if {
    behandelen with input.user.rollen as [ "behandelaar" ]
}

test_behandelen_with_coordinator_role if {
    behandelen with input.user.rollen as [ "coordinator" ]
}

test_behandelen_with_recordmanager_role if {
    behandelen with input.user.rollen as [ "recordmanager" ]
}

test_behandelen_with_beheerder_role if {
    behandelen with input.user.rollen as [ "beheerder" ]
}

test_behandelen_wrong_role_fails if {
    not behandelen with input.user.rollen as [ "fakeRole" ]
}

test_behandelen_missing_role_fails if {
    not behandelen with input.user.key as "value"
}

##########
# afbreken
##########
test_afbreken if {
    afbreken with input.user.rollen as [ "behandelaar" ]
}

test_afbreken_with_coordinator_role if {
    afbreken with input.user.rollen as [ "coordinator" ]
}

test_afbreken_with_recordmanager_role if {
    afbreken with input.user.rollen as [ "recordmanager" ]
}

test_afbreken_with_beheerder_role if {
    afbreken with input.user.rollen as [ "beheerder" ]
}

test_afbreken_wrong_role_fails if {
    not afbreken with input.user.rollen as [ "fakeRole" ]
}

test_afbreken_missing_role_fails if {
    not afbreken with input.user.key as "value"
}

###########
# heropenen
###########
test_heropenen if {
    heropenen with input.user.rollen as [ "recordmanager" ]
}

test_heropenen_with_beheerder_role if {
    heropenen with input.user.rollen as [ "beheerder" ]
}

test_heropenen_wrong_role_fails if {
    not heropenen with input.user.rollen as [ "fakeRole" ]
}

test_heropenen_missing_role_fails if {
    not heropenen with input.user.key as "value"
}

###################
# bekijken_zaakdata
###################
test_bekijken_zaakdata if {
    bekijken_zaakdata with input.user.rollen as [ "beheerder" ]
}

test_bekijken_zaakdata_wrong_role_fails if {
    not bekijken_zaakdata with input.user.rollen as [ "behandelaar" ]
}

test_bekijken_zaakdata_missing_role_fails if {
    not bekijken_zaakdata with input.user.key as "value"
}

#######################
# wijzigen_doorlooptijd
#######################
test_wijzigen_doorlooptijd if {
    wijzigen_doorlooptijd
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_wijzigen_doorlooptijd_with_coordinator_role if {
    wijzigen_doorlooptijd
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_wijzigen_doorlooptijd_with_recordmanager_role if {
    wijzigen_doorlooptijd
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_wijzigen_doorlooptijd_with_beheerder_role if {
    wijzigen_doorlooptijd
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
}

test_wijzigen_doorlooptijd_wrong_role_fails if {
    not wijzigen_doorlooptijd
        with input.user.rollen as [ "fakeRole" ]
        with input.zaak.open as true
}

test_wijzigen_doorlooptijd_missing_role_fails if {
    not wijzigen_doorlooptijd
        with input.user.key as "value"
        with input.zaak.open as true
}

test_wijzigen_doorlooptijd_zaak_closed_fails if {
    not wijzigen_doorlooptijd
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

###########
# verlengen
###########
test_verlengen if {
    verlengen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
        with input.zaak.verlengd as false
}

test_verlengen_with_coordinator_role if {
    verlengen
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
        with input.zaak.verlengd as false
}

test_verlengen_with_recordmanager_role if {
    verlengen
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
        with input.zaak.verlengd as false
}

test_verlengen_with_beheerder_role if {
    verlengen
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
        with input.zaak.verlengd as false
}

test_verlengen_zaak_closed_fails if {
    not verlengen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
        with input.zaak.verlengd as false
}

test_verlengen_heropend_fails if {
    not verlengen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.heropend as true
        with input.zaak.opgeschort as false
        with input.zaak.verlengd as false
}

test_verlengen_opgeschort_fails if {
    not verlengen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as true
        with input.zaak.verlengd as false
}

test_verlengen_verlengd_fails if {
    not verlengen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as true
        with input.zaak.verlengd as true
}

test_verlengen_wrong_role_fails if {
    not verlengen with input.user.rollen as [ "fakeRole" ]
}

test_verlengen_missing_role_fails if {
    not verlengen with input.user.key as "value"
}

############
# opschorten
############
test_opschorten if {
    opschorten
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
}

test_opschorten_with_coordinator_role if {
    opschorten
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
}

test_opschorten_with_recordmanager_role if {
    opschorten
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
}

test_opschorten_with_beheerder_role if {
    opschorten
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
}

test_opschorten_zaak_closed_fails if {
    not opschorten
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
        with input.zaak.heropend as false
        with input.zaak.opgeschort as false
}

test_opschorten_heropend_fails if {
    not opschorten
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.heropend as true
        with input.zaak.opgeschort as false
}

test_opschorten_opgeschort_fails if {
    not opschorten
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.heropend as false
        with input.zaak.opgeschort as true
}

test_opschorten_wrong_role_fails if {
    not opschorten with input.user.rollen as [ "fakeRole" ]
}

test_opschorten_missing_role_fails if {
    not opschorten with input.user.key as "value"
}

###########
# hervatten
###########
test_hervatten if {
    hervatten with input.user.rollen as [ "behandelaar" ]
}

test_hervatten_with_coordinator_role if {
    hervatten with input.user.rollen as [ "coordinator" ]
}

test_hervatten_with_recordmanager_role if {
    hervatten with input.user.rollen as [ "recordmanager" ]
}

test_hervatten_with_beheerder_role if {
    hervatten with input.user.rollen as [ "beheerder" ]
}

test_hervatten_wrong_role_fails if {
    not hervatten with input.user.rollen as [ "fakeRole" ]
}

test_hervatten_missing_role_fails if {
    not hervatten with input.user.key as "value"
}

###################
# creeren_document
###################
test_creeren_document if {
    creeren_document
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_creeren_document_with_coordinator_role if {
    creeren_document
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_creeren_document_with_recordmanager_role if {
    creeren_document
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_creeren_document_with_beheerder_role if {
    creeren_document
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
}

test_creeren_document_zaak_closed_fails if {
    not creeren_document
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_creeren_document_wrong_role_fails if {
    not creeren_document with input.user.rollen as [ "fakeRole" ]
}

test_creeren_document_missing_role_fails if {
    not creeren_document with input.user.key as "value"
}

####################
# toevoegen_document
####################
test_toevoegen_document_behandelaar if {
    toevoegen_document
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_toevoegen_document_behandelaar_zaak_closed_fails if {
    not toevoegen_document
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_toevoegen_document_coordinator if {
    toevoegen_document
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_toevoegen_document_coordinator_zaak_closed_fails if {
    not toevoegen_document
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_toevoegen_document_recordmanager if {
    toevoegen_document
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_toevoegen_document_beheerder if {
    toevoegen_document
        with input.user.rollen as [ "beheerder" ]
}

test_toevoegen_document_wrong_role_fails if {
    not toevoegen_document with input.user.rollen as [ "fakeRole" ]
}

test_toevoegen_document_missing_role_fails if {
    not toevoegen_document with input.user.key as "value"
}

##########
# koppelen
##########
test_koppelen if {
    koppelen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_koppelen_zaak_closed_fails if {
    not koppelen
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_koppelen_coordinator if {
    koppelen
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_koppelen_coordinator_zaak_closed_fails if {
    not koppelen
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_koppelen_recordmanager if {
    koppelen
        with input.user.rollen as [ "recordmanager" ]
}

test_koppelen_beheerder if {
    koppelen
        with input.user.rollen as [ "beheerder" ]
}

test_koppelen_wrong_role_fails if {
    not koppelen with input.user.rollen as [ "fakeRole" ]
}

test_koppelen_missing_role_fails if {
    not koppelen with input.user.key as "value"
}


#################
# versturen_email
#################
test_versturen_email if {
    versturen_email
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_versturen_email_with_coordinator_role if {
    versturen_email
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_versturen_email_with_recordmanager_role if {
    versturen_email
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_versturen_email_with_beheerder_role if {
    versturen_email
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
}

test_versturen_email_zaak_closed_fails if {
    not versturen_email
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_versturen_email_wrong_role_fails if {
    not versturen_email with input.user.rollen as [ "fakeRole" ]
}

test_versturen_email_missing_role_fails if {
    not versturen_email with input.user.key as "value"
}

################################
# versturen_ontvangstbevestiging
################################
test_versturen_ontvangstbevestiging if {
    versturen_ontvangstbevestiging
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_versturen_ontvangstbevestiging_with_coordinator_role if {
    versturen_ontvangstbevestiging
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_versturen_ontvangstbevestiging_with_recordmanager_role if {
    versturen_ontvangstbevestiging
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_versturen_ontvangstbevestiging_with_beheerder_role if {
    versturen_ontvangstbevestiging
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
}

test_versturen_ontvangstbevestiging_zaak_closed_fails if {
    not versturen_ontvangstbevestiging
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_versturen_ontvangstbevestiging_wrong_role_fails if {
    not versturen_ontvangstbevestiging with input.user.rollen as [ "fakeRole" ]
}

test_versturen_ontvangstbevestiging_missing_role_fails if {
    not versturen_ontvangstbevestiging with input.user.key as "value"
}

#############################
# toevoegen_initiator_persoon
#############################
test_toevoegen_initiator_persoon_behandelaar if {
    toevoegen_initiator_persoon
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_toevoegen_initiator_persoon_behandelaar_zaak_closed_fails if {
    not toevoegen_initiator_persoon
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_toevoegen_initiator_persoon_coordinator if {
    toevoegen_initiator_persoon
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_toevoegen_initiator_persoon_coordinator_zaak_closed_fails if {
    not toevoegen_initiator_persoon
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_toevoegen_initiator_persoon_recordmanager if {
    toevoegen_initiator_persoon
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_initiator_persoon_beheerder if {
    toevoegen_initiator_persoon
        with input.user.rollen as [ "beheerder" ]
}

test_toevoegen_initiator_persoon_wrong_role_fails if {
    not toevoegen_initiator_persoon with input.user.rollen as [ "fakeRole" ]
}

test_toevoegen_initiator_persoon_missing_role_fails if {
    not toevoegen_initiator_persoon with input.user.key as "value"
}

#############################
# toevoegen_initiator_bedrijf
#############################
test_toevoegen_initiator_bedrijf_behandelaar if {
    toevoegen_initiator_bedrijf
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_toevoegen_initiator_bedrijf_behandelaar_zaak_closed_fails if {
    not toevoegen_initiator_bedrijf
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_toevoegen_initiator_bedrijf_coordinator if {
    toevoegen_initiator_bedrijf
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_toevoegen_initiator_bedrijf_coordinator_zaak_closed_fails if {
    not toevoegen_initiator_bedrijf
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_toevoegen_initiator_bedrijf_recordmanager if {
    toevoegen_initiator_bedrijf
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_initiator_bedrijf_beheerder if {
    toevoegen_initiator_bedrijf
        with input.user.rollen as [ "beheerder" ]
}

test_toevoegen_initiator_bedrijf_wrong_role_fails if {
    not toevoegen_initiator_bedrijf with input.user.rollen as [ "fakeRole" ]
}

test_toevoegen_initiator_bedrijf_missing_role_fails if {
    not toevoegen_initiator_bedrijf with input.user.key as "value"
}

#######################
# verwijderen_initiator
#######################
test_verwijderen_initiator_behandelaar if {
    verwijderen_initiator
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_verwijderen_initiator_behandelaar_zaak_closed_fails if {
    not verwijderen_initiator
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_verwijderen_initiator_coordinator if {
    verwijderen_initiator
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_verwijderen_initiator_coordinator_zaak_closed_fails if {
    not verwijderen_initiator
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_verwijderen_initiator_recordmanager if {
    verwijderen_initiator
        with input.user.rollen as [ "recordmanager" ]
}

test_verwijderen_initiator_beheerder if {
    verwijderen_initiator
        with input.user.rollen as [ "beheerder" ]
}

test_verwijderen_initiator_wrong_role_fails if {
    not verwijderen_initiator with input.user.rollen as [ "fakeRole" ]
}

test_verwijderen_initiator_missing_role_fails if {
    not verwijderen_initiator with input.user.key as "value"
}

##############################
# toevoegen_betrokkene_persoon
##############################
test_toevoegen_betrokkene_persoon_behandelaar if {
    toevoegen_betrokkene_persoon
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_toevoegen_betrokkene_persoon_behandelaar_zaak_closed_fails if {
    not toevoegen_betrokkene_persoon
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_toevoegen_betrokkene_persoon_coordinator if {
    toevoegen_betrokkene_persoon
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_toevoegen_betrokkene_persoon_coordinator_zaak_closed_fails if {
    not toevoegen_betrokkene_persoon
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_toevoegen_betrokkene_persoon_recordmanager if {
    toevoegen_betrokkene_persoon
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_betrokkene_persoon_beheerder if {
    toevoegen_betrokkene_persoon
        with input.user.rollen as [ "beheerder" ]
}

test_toevoegen_betrokkene_persoon_wrong_role_fails if {
    not toevoegen_betrokkene_persoon with input.user.rollen as [ "fakeRole" ]
}

test_toevoegen_betrokkene_persoon_missing_role_fails if {
    not toevoegen_betrokkene_persoon with input.user.key as "value"
}

##############################
# toevoegen_betrokkene_bedrijf
##############################
test_toevoegen_betrokkene_bedrijf_behandelaar if {
    toevoegen_betrokkene_bedrijf
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_toevoegen_betrokkene_bedrijf_behandelaar_zaak_closed_fails if {
    not toevoegen_betrokkene_bedrijf
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_toevoegen_betrokkene_bedrijf_coordinator if {
    toevoegen_betrokkene_bedrijf
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_toevoegen_betrokkene_bedrijf_coordinator_zaak_closed_fails if {
    not toevoegen_betrokkene_bedrijf
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_toevoegen_betrokkene_bedrijf_recordmanager if {
    toevoegen_betrokkene_bedrijf
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_betrokkene_bedrijf_beheerder if {
    toevoegen_betrokkene_bedrijf
        with input.user.rollen as [ "beheerder" ]
}

test_toevoegen_betrokkene_bedrijf_wrong_role_fails if {
    not toevoegen_betrokkene_bedrijf with input.user.rollen as [ "fakeRole" ]
}

test_toevoegen_betrokkene_bedrijf_missing_role_fails if {
    not toevoegen_betrokkene_bedrijf with input.user.key as "value"
}

########################
# verwijderen_betrokkene
########################
test_verwijderen_betrokkene_behandelaar if {
    verwijderen_betrokkene
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_verwijderen_betrokkene_behandelaar_zaak_closed_fails if {
    not verwijderen_betrokkene
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_verwijderen_betrokkene_coordinator if {
    verwijderen_betrokkene
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_verwijderen_betrokkene_coordinator_zaak_closed_fails if {
    not verwijderen_betrokkene
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_verwijderen_betrokkene_recordmanager if {
    verwijderen_betrokkene
        with input.user.rollen as [ "recordmanager" ]
}

test_verwijderen_betrokkene_beheerder if {
    verwijderen_betrokkene
        with input.user.rollen as [ "beheerder" ]
}

test_verwijderen_betrokkene_wrong_role_fails if {
    not verwijderen_betrokkene with input.user.rollen as [ "fakeRole" ]
}

test_verwijderen_betrokkene_missing_role_fails if {
    not verwijderen_betrokkene with input.user.key as "value"
}

######################
# toevoegen_bag_object
######################
test_toevoegen_bag_object_behandelaar if {
    toevoegen_bag_object
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_toevoegen_bag_object_behandelaar_zaak_closed_fails if {
    not toevoegen_bag_object
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_toevoegen_bag_object_coordinator if {
    toevoegen_bag_object
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_toevoegen_bag_object_coordinator_zaak_closed_fails if {
    not toevoegen_bag_object
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as false
}

test_toevoegen_bag_object_recordmanager if {
    toevoegen_bag_object
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_bag_object_beheerder if {
    toevoegen_bag_object
        with input.user.rollen as [ "beheerder" ]
}

test_toevoegen_bag_object_wrong_role_fails if {
    not toevoegen_bag_object with input.user.rollen as [ "fakeRole" ]
}

test_toevoegen_bag_object_missing_role_fails if {
    not toevoegen_bag_object with input.user.key as "value"
}

##############
# starten_taak
##############
test_starten_taak if {
    starten_taak
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_starten_taak_with_coordinator_role if {
    starten_taak
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_starten_taak_with_recordmanager_role if {
    starten_taak
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_starten_taak_with_beheerder_role if {
    starten_taak
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
}

test_starten_taak_zaak_closed_fails if {
    not starten_taak
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_starten_taak_wrong_role_fails if {
    not starten_taak with input.user.rollen as [ "fakeRole" ]
}

test_starten_taak_missing_role_fails if {
    not starten_taak with input.user.key as "value"
}

####################
# vastleggen_besluit
####################
test_vastleggen_besluit if {
    vastleggen_besluit
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.intake as false
        with input.zaak.besloten as true
}

test_vastleggen_besluit_with_coordinator_role if {
    vastleggen_besluit
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
        with input.zaak.intake as false
        with input.zaak.besloten as true
}

test_vastleggen_besluit_with_recordmanager_role if {
    vastleggen_besluit
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
        with input.zaak.intake as false
        with input.zaak.besloten as true
}

test_vastleggen_besluit_with_beheerder_role if {
    vastleggen_besluit
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
        with input.zaak.intake as false
        with input.zaak.besloten as true
}

test_vastleggen_besluit_no_intake_and_besloten_fails if {
    not vastleggen_besluit
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_vastleggen_besluit_zaak_closed_fails if {
    not vastleggen_besluit
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
        with input.zaak.intake as false
        with input.zaak.besloten as true
}

test_vastleggen_besluit_in_intake_fails if {
    not vastleggen_besluit
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.intake as true
        with input.zaak.besloten as true
}

test_vastleggen_besluit_not_besluitd_fails if {
    not vastleggen_besluit
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
        with input.zaak.intake as false
        with input.zaak.besloten as false
}

test_vastleggen_besluit_wrong_role_fails if {
    not vastleggen_besluit with input.user.rollen as [ "fakeRole" ]
}

test_vastleggen_besluit_missing_role_fails if {
    not vastleggen_besluit with input.user.key as "value"
}

########################
# verlengen_doorlooptijd
########################
test_verlengen_doorlooptijd if {
    verlengen_doorlooptijd
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_verlengen_doorlooptijd_with_coordinator_role if {
    verlengen_doorlooptijd
        with input.user.rollen as [ "coordinator" ]
        with input.zaak.open as true
}

test_verlengen_doorlooptijd_with_recordmanager_role if {
    verlengen_doorlooptijd
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_verlengen_doorlooptijd_with_beheerder_role if {
    verlengen_doorlooptijd
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as true
}

test_verlengen_doorlooptijd_zaak_closed_fails if {
    not verlengen_doorlooptijd
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_verlengen_doorlooptijd_wrong_role_fails if {
    not verlengen_doorlooptijd with input.user.rollen as [ "fakeRole" ]
}

test_verlengen_doorlooptijd_missing_role_fails if {
    not verlengen_doorlooptijd with input.user.key as "value"
}

########################
# wijzigen_locatie
########################
test_wijzigen_locatie if {
    wijzigen_locatie
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_wijzigen_locatie_zaak_closed_fails if {
    not wijzigen_locatie
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_wijzigen_locatie_close_case_recordmanager if {
    wijzigen_locatie
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as false
}

test_wijzigen_locatie_close_case_beheerder if {
    wijzigen_locatie
        with input.user.rollen as [ "beheerder" ]
        with input.zaak.open as false
}

test_wijzigen_locatie_wrong_role_fails if {
    not wijzigen_locatie
        with input.user.rollen as [ "fakeRole" ]
        with input.zaak.open as false
}

########################
# brondatum_zetten
########################
test_brondatum_zetten_recordmanager if {
    brondatum_zetten
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as false
        with input.zaak.brondatumBepaald as false
}

test_brondatum_zetten_zaak_open_fails if {
    not brondatum_zetten
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
        with input.zaak.brondatumBepaald as false
}

test_brondatum_zetten_already_bepaald_fails if {
    not brondatum_zetten
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as false
        with input.zaak.brondatumBepaald as true
}

test_brondatum_zetten_wrong_role_fails if {
    not brondatum_zetten
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
        with input.zaak.brondatumBepaald as false
}

test_brondatum_zetten_missing_role_fails if {
    not brondatum_zetten with input.user.key as "value"
}

##################################
# zaak_allowed / zaakspecifiek geautoriseerde zaak
##################################
test_zaak_allowed_not_geautoriseerd if {
    zaak_allowed with input.zaak.zaakspecifiekGeautoriseerd as false
}

test_zaak_allowed_missing_field if {
    zaak_allowed with input.user.key as "value"
}

test_zaak_allowed_geautoriseerd_with_zaakspecifiek_geautoriseerd_role if {
    zaak_allowed
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_zaak_allowed_geautoriseerd_without_role_fails if {
    not zaak_allowed
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar" ]
}

test_lezen_geautoriseerd_behandelaar_without_flag_fails if {
    not lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar" ]
}

test_lezen_geautoriseerd_raadpleger_without_flag_fails if {
    not lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "raadpleger" ]
}

test_lezen_geautoriseerd_coordinator_without_flag_fails if {
    not lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "coordinator" ]
}

# the zaakspecifiek_geautoriseerd role is a flag, not a rights-bearing role: held alone, without
# also holding a normal application role such as behandelaar, it grants no rights at all
test_lezen_geautoriseerd_flag_alone_fails if {
    not lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_lezen_geautoriseerd_behandelaar_with_flag if {
    lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar", "zaakspecifiek_geautoriseerd" ]
}

test_lezen_geautoriseerd_raadpleger_with_flag if {
    lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "raadpleger", "zaakspecifiek_geautoriseerd" ]
}

test_lezen_geautoriseerd_coordinator_with_flag if {
    lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "coordinator", "zaakspecifiek_geautoriseerd" ]
}

test_lezen_geautoriseerd_recordmanager_unaffected if {
    lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "recordmanager" ]
}

test_lezen_geautoriseerd_beheerder_unaffected if {
    lezen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "beheerder" ]
}

test_wijzigen_geautoriseerd_behandelaar_without_flag_fails if {
    not wijzigen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.zaak.open as true
        with input.user.rollen as [ "behandelaar" ]
}

test_wijzigen_geautoriseerd_flag_alone_fails if {
    not wijzigen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.zaak.open as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_wijzigen_geautoriseerd_behandelaar_with_flag if {
    wijzigen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.zaak.open as true
        with input.user.rollen as [ "behandelaar", "zaakspecifiek_geautoriseerd" ]
}

test_wijzigen_geautoriseerd_recordmanager_unaffected if {
    wijzigen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "recordmanager" ]
}

test_behandelen_geautoriseerd_behandelaar_without_flag_fails if {
    not behandelen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar" ]
}

test_behandelen_geautoriseerd_flag_alone_fails if {
    not behandelen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_behandelen_geautoriseerd_behandelaar_with_flag if {
    behandelen
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar", "zaakspecifiek_geautoriseerd" ]
}

test_afbreken_geautoriseerd_behandelaar_without_flag_fails if {
    not afbreken
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar" ]
}

test_afbreken_geautoriseerd_flag_alone_fails if {
    not afbreken
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_afbreken_geautoriseerd_behandelaar_with_flag if {
    afbreken
        with input.zaak.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar", "zaakspecifiek_geautoriseerd" ]
}

# the zaakspecifiek_geautoriseerd flag grants no rights at all when held without a normal role
test_heropenen_zaakspecifiek_geautoriseerd_flag_alone_fails if {
    not heropenen with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_bekijken_zaakdata_zaakspecifiek_geautoriseerd_flag_alone_fails if {
    not bekijken_zaakdata with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_brondatum_zetten_zaakspecifiek_geautoriseerd_flag_alone_fails if {
    not brondatum_zetten
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
        with input.zaak.open as false
        with input.zaak.brondatumBepaald as false
}
