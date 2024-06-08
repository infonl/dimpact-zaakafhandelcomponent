#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.overig

import future.keywords
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.recordmanager
import input.user

overige_rechten := {
    "starten_zaak": starten_zaak,
    "beheren": beheren,
    "zoeken": zoeken,
    "sjabloon_toewijzing": sjabloon_toewijzing
}

default starten_zaak := false
starten_zaak {
    behandelaar.rol in user.rollen
}

default beheren:= false
beheren {
    beheerder.rol in user.rollen
}

default zoeken := false
zoeken {
    behandelaar.rol in user.rollen
}

default sjabloon_toewijzing := false
sjabloon_toewijzing {
    beheerder.rol in user.rollen
}
