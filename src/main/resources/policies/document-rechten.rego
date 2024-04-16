#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.document

import future.keywords
import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.behandelaar
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
    "ondertekenen": ondertekenen
}

default zaaktype_allowed := false
zaaktype_allowed {
    not document.zaaktype
}
zaaktype_allowed {
    not user.zaaktypen
}
zaaktype_allowed {
    document.zaaktype in user.zaaktypen
}

default onvergrendeld_of_vergrendeld_door_user := false
onvergrendeld_of_vergrendeld_door_user {
    document.vergrendeld == false
}
onvergrendeld_of_vergrendeld_door_user {
    document.vergrendeld == true
    document.vergrendeld_door == user.id
}

default lezen := false
lezen {
    { behandelaar, coordinator, recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
}

default wijzigen := false
wijzigen {
    { behandelaar, coordinator }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
    document.vergrendeld == false
}
wijzigen {
    { recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
}

default verwijderen := false
verwijderen {
    { behandelaar, coordinator }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
verwijderen {
    { recordmanager, beheerder }[_].rol in user.rollen
    document.zaak_open == true
}

default vergrendelen := false
vergrendelen {
    { behandelaar, coordinator, recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}

default ontgrendelen := false
ontgrendelen {
    { behandelaar, coordinator }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.vergrendeld_door == user.id
}
ontgrendelen {
    { recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}

default ondertekenen := false
ondertekenen {
    { behandelaar, coordinator, recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
}

default toevoegen_nieuwe_versie := fasle
toevoegen_nieuwe_versie {
    { behandelaar, coordinator }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
    document.definitief == false
}
toevoegen_nieuwe_versie {
    { recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
    # how can we check for signed documents?
}

default verplaatsen := false
verplaatsen {
    { behandelaar, coordinator }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
    document.definitief == false
}
verplaatsen {
    { recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
}

default ontkoppelen := false
ontkoppelen {
    { behandelaar, coordinator }[_].rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
    document.definitief == false
}
ontkoppelen {
    { recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
}

default downloaden := false
ontkoppelen {
    { behandelaar, coordinator, recordmanager, beheerder }[_].rol in user.rollen
    zaaktype_allowed
}
