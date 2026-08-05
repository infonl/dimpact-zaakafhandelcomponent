#
# SPDX-FileCopyrightText: 2025 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.notitie

import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager
import input.user

notitie_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen
}

default lezen := false
lezen if {
    raadpleger.rol in user.rollen
}

lezen if {
    behandelaar.rol in user.rollen
}

lezen if {
    coordinator.rol in user.rollen
}

lezen if {
    recordmanager.rol in user.rollen
}

lezen if {
    beheerder.rol in user.rollen
}

default wijzigen := false
wijzigen if {
    behandelaar.rol in user.rollen
}

wijzigen if {
    coordinator.rol in user.rollen
}

wijzigen if {
    recordmanager.rol in user.rollen
}

wijzigen if {
    beheerder.rol in user.rollen
}
