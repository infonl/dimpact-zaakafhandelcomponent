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
import data.net.atos.zac.rol.zaakspecifiekGeautoriseerd
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
# whose zaak is not zaakspecifiek geautoriseerd, otherwise only for a user who also holds the
# zaakspecifiek_geautoriseerd application role - regardless of which other application role(s)
# (including recordmanager or beheerder) the user holds.
default zaak_allowed := false
zaak_allowed if {
    not taak.zaakspecifiekGeautoriseerd
}
zaak_allowed if {
    zaakspecifiekGeautoriseerd.rol in user.rollen
}

default lezen := false
lezen if {
    zaaktype_allowed
    zaak_allowed
    some role in {raadpleger, behandelaar, coordinator, recordmanager, beheerder}
    role.rol in user.rollen
}

default wijzigen := false
wijzigen if {
    zaaktype_allowed
    zaak_allowed
    some role in {behandelaar, coordinator, recordmanager, beheerder}
    role.rol in user.rollen
}

default toekennen := false
toekennen if {
    zaaktype_allowed
    zaak_allowed
    some role in {behandelaar, coordinator, recordmanager, beheerder}
    role.rol in user.rollen
}

default creeren_document := false
creeren_document if {
    zaaktype_allowed
    taak.open
    zaak_allowed
    some role in {behandelaar, coordinator, recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_document := false
toevoegen_document if {
    zaaktype_allowed
    taak.open
    zaak_allowed
    some role in {behandelaar, coordinator, recordmanager, beheerder}
    role.rol in user.rollen
}
