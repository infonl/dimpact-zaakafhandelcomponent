#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.document

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.coordinator
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
    "downloaden": downloaden
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
    behandelaar.rol in user.rollen
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
    recordmanager.rol in user.rollen
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
    recordmanager.rol in user.rollen
    document.vergrendeld == false
}

default vergrendelen := false
vergrendelen if {
    behandelaar.rol in user.rollen
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
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default ondertekenen := false
ondertekenen if {
    behandelaar.rol in user.rollen
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
    recordmanager.rol in user.rollen
    zaaktype_allowed
    document.ondertekend == false
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
    recordmanager.rol in user.rollen
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
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default downloaden := false
downloaden if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}
