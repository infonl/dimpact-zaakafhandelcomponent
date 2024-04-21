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
import data.net.atos.zac.zaak.wijzigenZaakdata
import data.net.atos.zac.zaak.wijzigenDoorlooptijd
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

##################
# wijzigenZaakdata
##################
test_wijzigenZaakdata if {
    wijzigenZaakdata with input.user.rollen as [ "behandelaar" ]
}

test_wijzigenZaakdata_wrong_role_fails if {
    not wijzigenZaakdata with input.user.rollen as [ "functioneel" ]
}

test_wijzigenZaakdata_missing_role_fails if {
    not wijzigenZaakdata with input.user.key as "value"
}

######################
# wijzigenDoorlooptijd
######################
test_wijzigenDoorlooptijd if {
    wijzigenDoorlooptijd with input.user.rollen as [ "behandelaar" ]
}

test_wijzigenDoorlooptijd_wrong_role_fails if {
    not wijzigenDoorlooptijd with input.user.rollen as [ "functioneel" ]
}

test_wijzigenDoorlooptijd_missing_role_fails if {
    not wijzigenDoorlooptijd with input.user.key as "value"
}

###########
# verlengen
###########

############
# opschorten
############

###########
# hervatten
###########

###################
# creeeren_document
###################

####################
# toevoegen_document
####################

##########
# koppelen
##########

######################
# koppelen_gerelateerd
######################

#################
# versturen_email
#################

################################
# versturen_ontvangstbevestiging
################################

#############################
# toevoegen_initiator_persoon
#############################

#############################
# toevoegen_initiator_bedrijf
#############################

#######################
# verwijderen_initiator
#######################

##############################
# toevoegen_betrokkene_persoon
##############################

##############################
# toevoegen_betrokkene_bedrijf
##############################

########################
# verwijderen_betrokkene
########################

######################
# toevoegen_bag_object
######################

##############
# starten_taak
##############

####################
# vastleggen_besluit
####################

########################
# verlengen_doorlooptijd
########################
