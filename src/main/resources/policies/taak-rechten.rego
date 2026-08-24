#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.taak

import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager
import data.net.atos.zac.rol.zaakspecifiekAutorisatieBehandelaar
import input.user
import input.taak

taak_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen,
    "toekennen": toekennen,
    "creeren_document": creeren_document,
    "toevoegen_document": toevoegen_document
}

default zaaktype_allowed := false
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    taak.zaaktype in user.zaaktypen
}

# zaak_allowed guards access to a taak of a zaakspecifiek geautoriseerde zaak: unrestricted for a taak
# whose zaak is not geautoriseerd, otherwise only for a user holding zaakspecifiekAutorisatieBehandelaar.
# recordmanager/beheerder rule bodies below do not reference this rule - their access to such a taak is
# out of scope for this policy and is left unaffected.
default zaak_allowed := false
zaak_allowed if {
    not taak.zaakspecifiekGeautoriseerd
}
zaak_allowed if {
    zaakspecifiekAutorisatieBehandelaar.rol in user.rollen
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
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
wijzigen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toekennen := false
toekennen if {
    zaaktype_allowed
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
toekennen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default creeren_document := false
creeren_document if {
    zaaktype_allowed
    taak.open
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
creeren_document if {
    zaaktype_allowed
    taak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_document := false
toevoegen_document if {
    zaaktype_allowed
    taak.open
    zaak_allowed
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
toevoegen_document if {
    zaaktype_allowed
    taak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}
