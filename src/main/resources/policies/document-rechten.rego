#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.document

import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager
import data.net.atos.zac.rol.zaakspecifiekAutorisatieBehandelaar
import input.user
import input.document

document_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen,
    "verwijderen": verwijderen,
    "vergrendelen": vergrendelen,
    "ontgrendelen": ontgrendelen,
    "ondertekenen": ondertekenen,
    "toevoegen_nieuwe_versie": toevoegen_nieuwe_versie,
    "verplaatsen": verplaatsen,
    "ontkoppelen": ontkoppelen,
    "downloaden": downloaden,
    "converteren": converteren
}

default zaaktype_allowed := false
zaaktype_allowed if {
    not document.zaaktype
}
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    document.zaaktype in user.zaaktypen
}

# zaak_allowed guards access to a document of a zaakspecifiek geautoriseerde zaak: unrestricted for a
# document whose zaak is not geautoriseerd, otherwise only for a user holding
# zaakspecifiekAutorisatieBehandelaar. recordmanager/beheerder rule bodies below do not reference this rule -
# their access to such a document is out of scope for this policy and is left unaffected.
default zaak_allowed := false
zaak_allowed if {
    not document.zaakspecifiekGeautoriseerd
}
zaak_allowed if {
    zaakspecifiekAutorisatieBehandelaar.rol in user.rollen
}

default onvergrendeld_of_vergrendeld_door_user := false
onvergrendeld_of_vergrendeld_door_user if {
    document.vergrendeld == false
}
onvergrendeld_of_vergrendeld_door_user if {
    document.vergrendeld == true
    document.vergrendeld_door == user.id
}

default lezen := false
lezen if {
    zaaktype_allowed
    zaak_allowed
    some role in {raadpleger, behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
lezen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default wijzigen := false
wijzigen if {
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
wijzigen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default verwijderen := false
verwijderen if {
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    document.vergrendeld == false
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
verwijderen if {
    document.vergrendeld == false
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default vergrendelen := false
vergrendelen if {
    zaaktype_allowed
    document.zaak_open == true
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
vergrendelen if {
    zaaktype_allowed
    document.zaak_open == true
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default ontgrendelen := false
ontgrendelen if {
    zaaktype_allowed
    document.vergrendeld_door == user.id
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
ontgrendelen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default ondertekenen := false
ondertekenen if {
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
ondertekenen if {
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_nieuwe_versie := false
toevoegen_nieuwe_versie if {
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
toevoegen_nieuwe_versie if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default verplaatsen := false
verplaatsen if {
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
verplaatsen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default ontkoppelen := false
ontkoppelen if {
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
ontkoppelen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default downloaden := false
downloaden if {
    zaaktype_allowed
    zaak_allowed
    some role in {raadpleger, behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
downloaden if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default converteren := false
converteren if {
    document.definitief == true
    zaaktype_allowed
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
converteren if {
    document.definitief == true
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}
