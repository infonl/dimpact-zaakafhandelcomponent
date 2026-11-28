#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.taak

import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager
import input.user
import input.taak

taak_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen,
    "toekennen": toekennen,
    "creeeren_document": creeeren_document,
    "toevoegen_document": toevoegen_document
}

default zaaktype_allowed := false
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    taak.zaaktype in user.zaaktypen
}

default lezen := false
lezen if {
    raadpleger.rol in user.rollen
    zaaktype_allowed
}

default wijzigen := false
wijzigen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default toekennen := false
toekennen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}

default creeeren_document := false
creeeren_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    taak.open
}

default toevoegen_document := false
toevoegen_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    taak.open
}
