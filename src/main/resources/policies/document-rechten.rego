#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.document

import future.keywords
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
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default wijzigen := false
wijzigen {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
wijzigen {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default verwijderen := false
verwijderen {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    vergrendeld == false
}
verwijderen {
    recordmanager.rol in user.rollen
    document.zaak_open == true
    document.vergrendeld == false
}

default vergrendelen := false
vergrendelen {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}

default ontgrendelen := false
ontgrendelen {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.vergrendeld_door == user.id
}
ontgrendelen {
    recordmanager.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
}

default ondertekenen := false
ondertekenen {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    onvergrendeld_of_vergrendeld_door_user == true
}

default toevoegen_nieuwe_versie := false
toevoegen_nieuwe_versie {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
toevoegen_nieuwe_versie {
    recordmanager.rol in user.rollen
    zaaktype_allowed
    document.ondertekend == false
}

default verplaatsen := false
verplaatsen {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
verplaatsen {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default ontkoppelen := false
ontkoppelen {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    document.zaak_open == true
    document.definitief == false
    onvergrendeld_of_vergrendeld_door_user == true
}
ontkoppelen {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default downloaden := false
downloaden {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}
