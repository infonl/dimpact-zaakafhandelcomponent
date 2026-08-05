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
    raadpleger.rol in user.rollen
    zaaktype_allowed
}

lezen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

lezen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
}

lezen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

lezen if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default wijzigen := false
wijzigen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
wijzigen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
wijzigen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
wijzigen if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default verwijderen := false
verwijderen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    document.vergrendeld == false
}
verwijderen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    document.vergrendeld == false
}
verwijderen if {
    recordmanager.rol in user.rollen
    document.vergrendeld == false
}
verwijderen if {
    beheerder.rol in user.rollen
    document.vergrendeld == false
}

default vergrendelen := false
vergrendelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}
vergrendelen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}
vergrendelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}
vergrendelen if {
    beheerder.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}

default ontgrendelen := false
ontgrendelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.vergrendeld_door == user.id
}
ontgrendelen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.vergrendeld_door == user.id
}
ontgrendelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
ontgrendelen if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default ondertekenen := false
ondertekenen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
}
ondertekenen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
}
ondertekenen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
}
ondertekenen if {
    beheerder.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
}

default toevoegen_nieuwe_versie := false
toevoegen_nieuwe_versie if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
toevoegen_nieuwe_versie if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
toevoegen_nieuwe_versie if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toevoegen_nieuwe_versie if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default verplaatsen := false
verplaatsen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
verplaatsen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
verplaatsen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
verplaatsen if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default ontkoppelen := false
ontkoppelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
ontkoppelen if {
    coordinator.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
ontkoppelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
ontkoppelen if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default downloaden := false
downloaden if {
    raadpleger.rol in user.rollen
    zaaktype_allowed
}

downloaden if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

downloaden if {
    coordinator.rol in user.rollen
    zaaktype_allowed
}

downloaden if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

downloaden if {
    beheerder.rol in user.rollen
    zaaktype_allowed
}

default converteren := false
converteren if {
    behandelaar.rol in user.rollen
    document.definitief == true
    zaaktype_allowed
}
converteren if {
    coordinator.rol in user.rollen
    document.definitief == true
    zaaktype_allowed
}
converteren if {
    recordmanager.rol in user.rollen
    document.definitief == true
    zaaktype_allowed
}
converteren if {
    beheerder.rol in user.rollen
    document.definitief == true
    zaaktype_allowed
}
