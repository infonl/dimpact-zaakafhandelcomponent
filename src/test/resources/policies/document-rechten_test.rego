#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
package net.atos.zac.document

import rego.v1

import data.net.atos.zac.document.zaaktype_allowed
import data.net.atos.zac.document.zaak_allowed
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
import data.net.atos.zac.document.converteren

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

test_lezen_with_behandelaar_role if {
    lezen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_with_coordinator_role if {
    lezen
        with input.user.rollen as ["coordinator"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_with_recordmanager_role if {
    lezen
        with input.user.rollen as ["recordmanager"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_with_beheerder_role if {
    lezen
        with input.user.rollen as ["beheerder"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_missing_role_fails if {
    not lezen
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_wrong_role_fails if {
    not lezen
        with input.user.rollen as ["fakeRole"]
        with input.document.zaaktype as "type"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_zaaktype_not_allowed_fails if {
    not lezen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaaktype as "unknown"
        with input.user.zaaktypen as ["firstType", "type"]
}

test_lezen_wrong_role_zaaktype_not_allowed_fails if {
    not lezen
        with input.user.rollen as ["fakeRole"]
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

test_wijzigen_coordinator_unlocked if {
    wijzigen
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_behandelaar_missing_role_fails if {
    not wijzigen
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_wrong_role_fails if {
    not wijzigen
        with input.user.rollen as ["fakeRole"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_zaaktype_not_allowed_fails if {
    not wijzigen
        with input.user.rollen as ["fakeRole"]
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

test_wijzigen_beheerder if {
    wijzigen
        with input.user.rollen as ["beheerder"]
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

test_verwijderen_coordinator if {
    verwijderen
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_verwijderen_recordmanager if {
    verwijderen
        with input.user.rollen as ["recordmanager"]
        with input.document.vergrendeld as false
}

test_verwijderen_beheerder if {
    verwijderen
        with input.user.rollen as ["beheerder"]
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
        with input.user.rollen as ["fakeRole"]
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

test_vergrendelen_with_coordinator_role if {
    vergrendelen
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
}

test_vergrendelen_with_recordmanager_role if {
    vergrendelen
        with input.user.rollen as ["recordmanager"]
        with input.document.zaak_open as true
}

test_vergrendelen_with_beheerder_role if {
    vergrendelen
        with input.user.rollen as ["beheerder"]
        with input.document.zaak_open as true
}

test_vergrendelen_wrong_role_fails if {
    not vergrendelen
        with input.user.rollen as ["fakeRole"]
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

test_ontgrendelen_coordinator if {
    ontgrendelen
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
        with input.user.id as "1"
        with input.document.vergrendeld_door as "1"
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

test_ontgrendelen_beheerder if {
    ontgrendelen
        with input.user.rollen as ["beheerder"]
        with input.document.zaak_open as true
}

test_ontgrendelen_wrong_role_fails if {
    not ontgrendelen
        with input.user.rollen as ["fakeRole"]
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

test_ondertekenen_with_coordinator_role if {
    ondertekenen
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
        with input.document.vergrendeld as false
}

test_ondertekenen_with_recordmanager_role if {
    ondertekenen
        with input.user.rollen as ["recordmanager"]
        with input.document.zaak_open as true
        with input.document.vergrendeld as false
}

test_ondertekenen_with_beheerder_role if {
    ondertekenen
        with input.user.rollen as ["beheerder"]
        with input.document.zaak_open as true
        with input.document.vergrendeld as false
}

test_ondertekenen_wrong_role_fails if {
    not ondertekenen
        with input.user.rollen as ["fakeRole"]
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

test_toevoegen_nieuwe_versie_coordinator if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_toevoegen_nieuwe_versie_recordmanager if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["recordmanager"]
        with input.document.ondertekend as false
}

test_toevoegen_nieuwe_versie_recordmanager_ondertekend if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["recordmanager"]
        with input.document.ondertekend as true
}

test_toevoegen_nieuwe_versie_beheerder if {
    toevoegen_nieuwe_versie
        with input.user.rollen as ["beheerder"]
}

test_toevoegen_nieuwe_versie_wrong_role_fails if {
    not toevoegen_nieuwe_versie
        with input.user.rollen as ["fakeRole"]
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

test_verplaatsen_coordinator if {
    verplaatsen
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_verplaatsen_recordmanager if {
    verplaatsen
        with input.user.rollen as ["recordmanager"]
}

test_verplaatsen_beheerder if {
    verplaatsen
        with input.user.rollen as ["beheerder"]
}

test_verplaatsen_wrong_role_fails if {
    not verplaatsen
        with input.user.rollen as ["fakeRole"]
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

test_ontkoppelen_coordinator if {
    ontkoppelen
        with input.user.rollen as ["coordinator"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_ontkoppelen_recordmanager if  {
    ontkoppelen
        with input.user.rollen as ["recordmanager"]
}

test_ontkoppelen_beheerder if  {
    ontkoppelen
        with input.user.rollen as ["beheerder"]
}

test_ontkoppelen_missing_role_fails if {
    not ontkoppelen
        with input.document.zaak_open as true
}

test_ontkoppelen_wrong_role_fails if {
    not ontkoppelen
        with input.user.rollen as ["fakeRole"]
}

############
# downloaden
############
test_downloaden if  {
    downloaden
        with input.user.rollen as ["raadpleger"]
}

test_downloaden_with_behandelaar_role if {
    downloaden
        with input.user.rollen as ["behandelaar"]
}

test_downloaden_with_coordinator_role if {
    downloaden
        with input.user.rollen as ["coordinator"]
}

test_downloaden_with_recordmanager_role if {
    downloaden
        with input.user.rollen as ["recordmanager"]
}

test_downloaden_with_beheerder_role if {
    downloaden
        with input.user.rollen as ["beheerder"]
}

test_downloaden_missing_role_fails if {
    not downloaden
        with input.document.zaak_open as true
}

test_downloaden_wrong_role_fails if {
    not downloaden
        with input.user.rollen as ["fakeRole"]
}

############
# converteren
############
test_converteren if  {
    converteren
        with input.user.rollen as ["behandelaar"]
        with input.document.definitief as true
}

test_converteren_with_coordinator_role if {
    converteren
        with input.user.rollen as ["coordinator"]
        with input.document.definitief as true
}

test_converteren_with_recordmanager_role if {
    converteren
        with input.user.rollen as ["recordmanager"]
        with input.document.definitief as true
}

test_converteren_with_beheerder_role if {
    converteren
        with input.user.rollen as ["beheerder"]
        with input.document.definitief as true
}

test_converteren_wrong_role_fails if {
    not converteren
        with input.user.rollen as ["raadpleger"]
        with input.document.definitief as true
}

test_converteren_document_not_definitief_fails if {
    not converteren
        with input.user.rollen as ["behandelaar"]
        with input.document.definitief as false
}

##################################
# zaak_allowed / zaakspecifiek geautoriseerde zaak
##################################
test_zaak_allowed_not_geautoriseerd if {
    zaak_allowed with input.document.zaakspecifiekGeautoriseerd as false
}

test_zaak_allowed_geautoriseerd_with_zaakspecifiek_geautoriseerd_role if {
    zaak_allowed
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_zaak_allowed_geautoriseerd_without_role_fails if {
    not zaak_allowed
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar" ]
}

test_lezen_geautoriseerd_behandelaar_without_flag_fails if {
    not lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar" ]
}

test_lezen_geautoriseerd_raadpleger_without_flag_fails if {
    not lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "raadpleger" ]
}

test_lezen_geautoriseerd_coordinator_without_flag_fails if {
    not lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "coordinator" ]
}

# the zaakspecifiek_geautoriseerd role is a flag, not a rights-bearing role: held alone, without
# also holding a normal application role such as behandelaar, it grants no rights at all
test_lezen_geautoriseerd_flag_alone_fails if {
    not lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_lezen_geautoriseerd_behandelaar_with_flag if {
    lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar", "zaakspecifiek_geautoriseerd" ]
}

test_lezen_geautoriseerd_recordmanager_without_flag_fails if {
    not lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "recordmanager" ]
}

test_lezen_geautoriseerd_beheerder_without_flag_fails if {
    not lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "beheerder" ]
}

test_lezen_geautoriseerd_recordmanager_with_flag if {
    lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "recordmanager", "zaakspecifiek_geautoriseerd" ]
}

test_lezen_geautoriseerd_beheerder_with_flag if {
    lezen
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "beheerder", "zaakspecifiek_geautoriseerd" ]
}

test_downloaden_geautoriseerd_behandelaar_without_flag_fails if {
    not downloaden
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar" ]
}

test_downloaden_geautoriseerd_flag_alone_fails if {
    not downloaden
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "zaakspecifiek_geautoriseerd" ]
}

test_downloaden_geautoriseerd_behandelaar_with_flag if {
    downloaden
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "behandelaar", "zaakspecifiek_geautoriseerd" ]
}

test_downloaden_geautoriseerd_recordmanager_without_flag_fails if {
    not downloaden
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "recordmanager" ]
}

test_downloaden_geautoriseerd_recordmanager_with_flag if {
    downloaden
        with input.document.zaakspecifiekGeautoriseerd as true
        with input.user.rollen as [ "recordmanager", "zaakspecifiek_geautoriseerd" ]
}
