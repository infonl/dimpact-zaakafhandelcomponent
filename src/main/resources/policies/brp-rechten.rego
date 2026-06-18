#
# SPDX-FileCopyrightText: 2026 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.brp

import data.net.atos.zac.rol.brpZoeken
import input.user
import input.gemeente_code

brp_rechten := {
    "zoeken": brp_zoeken
}

default brp_zoeken := false

brp_zoeken if {
    gemeente_code
    gemeente_code in user.brpGemeenteCodes
}

brp_zoeken if {
    brpZoeken.rol in user.rollen
}