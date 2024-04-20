#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.overig

import rego.v1

import data.net.atos.zac.document.zaaktype_allowed
import data.net.atos.zac.document.onvergrendeld_of_vergrendeld_door_user
import data.net.atos.zac.document.lezen
import data.net.atos.zac.document.wijzigen

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

test_lezen if {
    lezen
        with input.user.rollen as ["behandelaar"]
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

test_wijzigen_missing_role_fails if {
    not wijzigen
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_bad_role_fails if {
    not wijzigen
        with input.user.rollen as ["functioneel"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_zaaktype_not_allowed_fails if {
    not wijzigen
        with input.user.rollen as ["functioneel"]
        with input.document.zaaktype as "unknown"
        with input.user.zaaktypen as ["firstType", "type"]
        with input.document.zaak_open as true
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_zaak_closed_fails if {
    not wijzigen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as false
        with input.document.definitief as false
        with input.document.vergrendeld as false
}

test_wijzigen_definitief_fails if {
    not wijzigen
        with input.user.rollen as ["behandelaar"]
        with input.document.zaak_open as true
        with input.document.definitief as true
        with input.document.vergrendeld as false
}

test_wijzigen_not_onvergrendeld_of_vergrendeld_door_user_fails if {
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
