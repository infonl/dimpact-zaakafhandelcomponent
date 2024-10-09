#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.overig

import rego.v1

import data.net.atos.zac.zaak.zaaktype_allowed
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
import data.net.atos.zac.zaak.creeeren_document
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
    lezen with input.user.rollen as [ "behandelaar" ]
}

test_lezen_wrong_role_fails if {
    not lezen with input.user.rollen as [ "functioneel" ]
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

test_wijzigen_recordmanager if {
    wijzigen
        with input.user.rollen as [ "recordmanager" ]
}

test_wijzigen_wrong_role_fails if {
    not wijzigen with input.user.rollen as [ "functioneel" ]
}

test_wijzigen_missing_role_fails if {
    not wijzigen with input.user.key as "value"
}

###########
# toekennen
###########
test_toekennen if {
    toekennen with input.user.rollen as [ "behandelaar" ]
}

test_toekennen_wrong_role_fails if {
    not toekennen with input.user.rollen as [ "functioneel" ]
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

test_behandelen_wrong_role_fails if {
    not behandelen with input.user.rollen as [ "functioneel" ]
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

test_afbreken_wrong_role_fails if {
    not afbreken with input.user.rollen as [ "functioneel" ]
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

test_heropenen_wrong_role_fails if {
    not heropenen with input.user.rollen as [ "functioneel" ]
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
    wijzigen_doorlooptijd with input.user.rollen as [ "behandelaar" ]
}

test_wijzigen_doorlooptijd_wrong_role_fails if {
    not wijzigen_doorlooptijd with input.user.rollen as [ "functioneel" ]
}

test_wijzigen_doorlooptijd_missing_role_fails if {
    not wijzigen_doorlooptijd with input.user.key as "value"
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
    not verlengen with input.user.rollen as [ "functioneel" ]
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
    not opschorten with input.user.rollen as [ "functioneel" ]
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

test_hervatten_wrong_role_fails if {
    not hervatten with input.user.rollen as [ "functioneel" ]
}

test_hervatten_missing_role_fails if {
    not hervatten with input.user.key as "value"
}

###################
# creeeren_document
###################
test_creeeren_document if {
    creeeren_document
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as true
}

test_creeeren_document_zaak_closed_fails if {
    not creeeren_document
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_creeeren_document_wrong_role_fails if {
    not creeeren_document with input.user.rollen as [ "functioneel" ]
}

test_creeeren_document_missing_role_fails if {
    not creeeren_document with input.user.key as "value"
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

test_toevoegen_document_recordmanager if {
    toevoegen_document
        with input.user.rollen as [ "recordmanager" ]
        with input.zaak.open as true
}

test_toevoegen_document_wrong_role_fails if {
    not toevoegen_document with input.user.rollen as [ "functioneel" ]
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

test_koppelen_recordmanager if {
    koppelen
        with input.user.rollen as [ "recordmanager" ]
}

test_koppelen_wrong_role_fails if {
    not koppelen with input.user.rollen as [ "functioneel" ]
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

test_versturen_email_zaak_closed_fails if {
    not versturen_email
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_versturen_email_wrong_role_fails if {
    not versturen_email with input.user.rollen as [ "functioneel" ]
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

test_versturen_ontvangstbevestiging_zaak_closed_fails if {
    not versturen_ontvangstbevestiging
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_versturen_ontvangstbevestiging_wrong_role_fails if {
    not versturen_ontvangstbevestiging with input.user.rollen as [ "functioneel" ]
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

test_toevoegen_initiator_persoon_recordmanager if {
    toevoegen_initiator_persoon
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_initiator_persoon_wrong_role_fails if {
    not toevoegen_initiator_persoon with input.user.rollen as [ "functioneel" ]
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

test_toevoegen_initiator_bedrijf_recordmanager if {
    toevoegen_initiator_bedrijf
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_initiator_bedrijf_wrong_role_fails if {
    not toevoegen_initiator_bedrijf with input.user.rollen as [ "functioneel" ]
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

test_verwijderen_initiator_recordmanager if {
    verwijderen_initiator
        with input.user.rollen as [ "recordmanager" ]
}

test_verwijderen_initiator_wrong_role_fails if {
    not verwijderen_initiator with input.user.rollen as [ "functioneel" ]
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

test_toevoegen_betrokkene_persoon_recordmanager if {
    toevoegen_betrokkene_persoon
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_betrokkene_persoon_wrong_role_fails if {
    not toevoegen_betrokkene_persoon with input.user.rollen as [ "functioneel" ]
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

test_toevoegen_betrokkene_bedrijf_recordmanager if {
    toevoegen_betrokkene_bedrijf
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_betrokkene_bedrijf_wrong_role_fails if {
    not toevoegen_betrokkene_bedrijf with input.user.rollen as [ "functioneel" ]
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

test_verwijderen_betrokkene_recordmanager if {
    verwijderen_betrokkene
        with input.user.rollen as [ "recordmanager" ]
}

test_verwijderen_betrokkene_wrong_role_fails if {
    not verwijderen_betrokkene with input.user.rollen as [ "functioneel" ]
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

test_toevoegen_bag_object_recordmanager if {
    toevoegen_bag_object
        with input.user.rollen as [ "recordmanager" ]
}

test_toevoegen_bag_object_wrong_role_fails if {
    not toevoegen_bag_object with input.user.rollen as [ "functioneel" ]
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

test_starten_taak_zaak_closed_fails if {
    not starten_taak
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_starten_taak_wrong_role_fails if {
    not starten_taak with input.user.rollen as [ "functioneel" ]
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
    not vastleggen_besluit with input.user.rollen as [ "functioneel" ]
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

test_verlengen_doorlooptijd_zaak_closed_fails if {
    not verlengen_doorlooptijd
        with input.user.rollen as [ "behandelaar" ]
        with input.zaak.open as false
}

test_verlengen_doorlooptijd_wrong_role_fails if {
    not verlengen_doorlooptijd with input.user.rollen as [ "functioneel" ]
}

test_verlengen_doorlooptijd_missing_role_fails if {
    not verlengen_doorlooptijd with input.user.key as "value"
}
