#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.overig

import rego.v1

import data.net.atos.zac.document.zaaktype_allowed
import data.net.atos.zac.document.onvergrendeld_of_vergrendeld_door_user
import data.net.atos.zac.document.lezen
import data.net.atos.zac.document.wijzigen
import data.net.atos.zac.document.verwijderen
import data.net.atos.zac.document.vergrendelen
import data.net.atos.zac.document.ontgrendelen
import data.net.atos.zac.document.ondertekenen
import data.net.atos.zac.document.toevoegen_nieuwe_versie
import data.net.atos.zac.document.verplaatsen
import data.net.atos.zac.document.ontkoppelen
import data.net.atos.zac.document.downloaden

##################
# zaaktype_allowed
##################
test_zaaktype_allowed_with_mising_doc_zaaktype if {
    zaaktype_allowed with input.document.key as "value"
    zaaktype_allowed with input.document.zaaktype as null
    zaaktype_allowed with input.document.zaaktype as ""
}

test_zaaktype_allowed_with_mising_user_zaaktypen_key if {
    zaaktype_allowed with input.user.key as "value"
    zaaktype_allowed with input.user.zaaktypen as null
    zaaktype_allowed with input.user.zaaktypen as ""
}

test_zaaktype_allowed_with_user_zaaktypen_and_missing_doc_zaaktype if {
    zaaktype_allowed with input.user.zaaktypen as ["type"]
}

test_zaaktype_allowed_with_doc_zaaktype_and_missing_user_zaaktypen if {
    zaaktype_allowed with input.document.zaaktype as ["type"]
}

test_zaaktype_allowed_with_doc_zaaktype_in_user_zaaktypen if {
    zaaktype_allowed
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_zaaktype_allowed_with_doc_zaaktype_not_in_user_zaaktypen_fails if {
    not zaaktype_allowed
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["unknown type"]
}

########################################
# onvergrendeld_of_vergrendeld_door_user
########################################
test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_false if {
    onvergrendeld_of_vergrendeld_door_user with input.document.vergrendeld as false
}

test_onvergrendeld_of_vergrendeld_door_user_missing_vergrendeld_fails if {
    not onvergrendeld_of_vergrendeld_door_user with input.document.key as "value"
}

test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_true_fails if {
    not onvergrendeld_of_vergrendeld_door_user with input.document.vergrendeld as true
}

test_onvergrendeld_of_vergrendeld_door_user if {
    onvergrendeld_of_vergrendeld_door_user
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as "1"
        with input.user.id as "1"
}

test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_true_and_vergrendeld_door_not_eq_user_id_fails if {
    not onvergrendeld_of_vergrendeld_door_user
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as "1"
        with input.user.id as "2"
}

test_onvergrendeld_of_vergrendeld_door_user_vergrendeld_true_and_vergrendeld_door_missing_fails if {
    not onvergrendeld_of_vergrendeld_door_user
        with input.document.vergrendeld as true
        with input.user.id as "2"
    not onvergrendeld_of_vergrendeld_door_user
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as null
        with input.user.id as "2"
    not onvergrendeld_of_vergrendeld_door_user
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as ""
        with input.user.id as "2"
}

#######
# lezen
#######
test_lezen if {
    lezen
        with input.user.rollen as ["raadpleger"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_missing_role_fails if {
    not lezen
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_bad_role_fails if {
    not lezen
        with input.user.rollen as ["functioneel"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_zaaktype_not_allowed_fails if {
    not lezen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaaktype as "unknown"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_bad_role_zaaktype_not_allowed_fails if {
    not lezen
        with input.user.rollen as ["functioneel"]
        with input.document.zaaktype as "unknown"
        with input.user.zaaktypen as ["firstType", "type"]
}

##########
# wijzigen
##########
test_wijzigen_behandelaar_unlocked if {
    wijzigen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.onvergrendeld_of_vergrendeld_door_user as true
        with input.document.vergrendeld as false
}

test_wijzigen_behandelaar_locked_by_user if {
    wijzigen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as "1"
        with input.user.id as "1"
}

test_wijzigen_behandelaar_missing_role_fails if {
    not wijzigen
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_behandelaar_bad_role_fails if {
    not wijzigen
        with input.user.rollen as ["functioneel"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_behandelaar_zaaktype_not_allowed_fails if {
    not wijzigen
        with input.user.rollen as ["functioneel"]
        with input.document.zaaktype as "unknown"
        with input.user.zaaktypen as ["firstType", "type"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_behandelaar_zaak_closed_fails if {
    not wijzigen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_behandelaar_definitief_fails if {
    not wijzigen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as false
}

test_wijzigen_behandelaar_not_onvergrendeld_of_vergrendeld_door_user_fails if {
    not wijzigen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as "2"
        with input.user.id as "1"
}

test_wijzigen_recordmanager if {
    wijzigen
        with input.user.rollen as ["recordmanager"]
}

test_wijzigen_recordmanager_zaaktype_not_allowed_fails if {
    not wijzigen
        with input.user.rollen as ["recordmanager"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["unknown type"]
}

#############
# verwijderen
#############
test_verwijderen_behandelaar if {
    verwijderen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_verwijderen_behandelaar_locked_by_this_user_fails if {
    not verwijderen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as "1"
        with input.user.id as "1"
}

test_verwijderen_behandelaar_zaak_closed_fails if {
    not verwijderen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_verwijderen_behandelaar_definitief_fails if {
    not verwijderen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as false
}

test_verwijderen_behandelaar_locked_by_other_user_fails if {
    not verwijderen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as true
        with input.document.vergrendeld_door as "2"
        with input.user.id as "1"
}

test_verwijderen_behandelaar_missing_role_fails if {
    not verwijderen
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as false
}

test_verwijderen_recordmanager if {
    verwijderen
        with input.user.rollen as ["recordmanager"]
        with input.document.vergrendeld as false
}

test_verwijderen_recordmanager_locked_fails if {
    not verwijderen
        with input.user.rollen as ["recordmanager"]
        with input.document.zaak_open as true
        with input.document.vergrendeld as true
}

test_verwijderen_recordmanager_missing_role_fails if {
    not verwijderen
        with input.document.zaak_open as true
}

test_verwijderen_wrong_role_fails if {
    not verwijderen
        with input.user.rollen as ["functioneel"]
        with input.document.zaak_open as false
}

##############
# vergrendelen
##############
test_vergrendelen if {
    vergrendelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
}

test_vergrendelen_wrong_role_fails if {
    not vergrendelen
        with input.user.rollen as ["functioneel"]
        with input.document.zaak_open as true
}

test_vergrendelen_zaak_closed_fails if {
    not vergrendelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
}

test_vergrendelen_role_missing_fails if {
    not vergrendelen
        with input.document.zaak_open as false
}

##############
# ontgrendelen
##############
test_ontgrendelen_behandelaar if {
    ontgrendelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "1"
}

test_ontgrendelen_behandelaar_zaak_closed if {
    ontgrendelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.user.id as "1"
        with input.document.vergrendeld_door as "1"
}

test_ontgrendelen_behandelaar_locked_by_other_user_fails if {
    not ontgrendelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.user.id as "1"
        with input.document.vergrendeld_door as "2"
}

test_ontgrendelen_recordmanager if {
    ontgrendelen
        with input.user.rollen as ["recordmanager"]
        with input.document.zaak_open as true
}

test_ontgrendelen_recordmanager_zaak_closed if {
    ontgrendelen
        with input.user.rollen as ["recordmanager"]
        with input.document.zaak_open as false
}

test_ontgrendelen_wrong_role_fails if {
    not ontgrendelen
        with input.user.rollen as ["functioneel"]
}

test_ontgrendelen_missing_role_fails if {
    not ontgrendelen
        with input.document.zaak_open as true
}

##############
# ondertekenen
##############
test_ondertekenen_behandelaar if  {
    ondertekenen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.vergrendeld as false
}

test_ondertekenen_behandelaar_locked_by_this_user if  {
    ondertekenen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "1"
}

test_ondertekenen_behandelaar_zaak_closed_fails if  {
    not ondertekenen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
}

test_ondertekenen_behandelaar_locked_by_another_user_fails if  {
    not ondertekenen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "2"
}

test_ondertekenen_wrong_role_fails if {
    not ondertekenen
        with input.user.rollen as ["functioneel"]
}

test_ondertekenen_missing_role_fails if {
    not ondertekenen
        with input.document.zaak_open as true
}

#########################
# toevoegen_nieuwe_versie
#########################
test_toevoegen_nieuwe_versie_behandelaar if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar_locked_by_current_user if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "1"
}

test_toevoegen_nieuwe_versie_behandelaar_zaak_closed_fails if {
    not toevoegen_nieuwe_versie
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar_definitief_fails if {
    not toevoegen_nieuwe_versie
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as false
}

test_toevoegen_nieuwe_versie_behandelaar_locked_by_other_user_fails if {
    not toevoegen_nieuwe_versie
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "2"
}

test_toevoegen_nieuwe_versie_recordmanager if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["recordmanager"]
        with input.document.ondertekend as false
}

test_toevoegen_nieuwe_versie_recordmanager_ondertekend_fails if {
    not toevoegen_nieuwe_versie
        with input.user.rollen as ["recordmanager"]
        with input.document.ondertekend as true
}

test_toevoegen_nieuwe_versie_wrong_role_fails if {
    not toevoegen_nieuwe_versie
        with input.user.rollen as ["functioneel"]
}

#############
# verplaatsen
#############
test_verplaatsen_behandelaar if {
    verplaatsen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_verplaatsen_behandelaar_locked_same_user if {
    verplaatsen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "1"
}

test_verplaatsen_behandelaar_zaak_closed_fails if {
    not verplaatsen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_verplaatsen_behandelaar_definitief_fails if {
    not verplaatsen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as false
}

test_verplaatsen_behandelaar_locked_other_user_fails if {
    not verplaatsen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "2"
}

test_verplaatsen_recordmanager if {
    verplaatsen
        with input.user.rollen as ["recordmanager"]
}

test_verplaatsen_behandelaar_wrong_role_fails if {
    not toevoegen_nieuwe_versie
        with input.user.rollen as ["functioneel"]
}

#############
# ontkoppelen
#############
test_ontkoppelen_behandelaar if {
    ontkoppelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_ontkoppelen_behandelaar_locked_by_current_user if {
    ontkoppelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "1"
}

test_ontkoppelen_behandelaar_zaak_closed_fails if {
    not ontkoppelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_ontkoppelen_behandelaar_definitief_fails if {
    not ontkoppelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as false
}

test_ontkoppelen_behandelaar_locked_by_another_user_fails if {
    not ontkoppelen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "2"
}

test_ontkoppelen_recordmanager if  {
    ontkoppelen
        with input.user.rollen as ["recordmanager"]
}

test_ontkoppelen_missing_role_fails if {
    not ontkoppelen
        with input.document.zaak_open as true
}

test_ontkoppelen_wrong_role_fails if {
    not ontkoppelen
        with input.user.rollen as ["functioneel"]
}

############
# downloaden
############
test_downloaden if  {
    downloaden
        with input.user.rollen as ["raadpleger"]
}

test_ontkoppelen_missing_role_fails if {
    not downloaden
        with input.document.zaak_open as true
}

test_ontkoppelen_wrong_role_fails if {
    not downloaden
        with input.user.rollen as ["functioneel"]
}
