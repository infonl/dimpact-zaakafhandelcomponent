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

# IF the auth request is for a specific gemeenteCode
# AND the user has that gemeenteCode in the list of gemeenteCodes they are autorized for
# BECAUSE they have the brp_zoeken role in PABC for a domain that contains that gemeente
# THEN the auth request succeeds
brp_zoeken if {
    gemeente_code
    gemeente_code in user.brpGemeenteCodes
}

# REGARDLESS of whether the request is for a specific gemeenteCode or across gemeenten
# IF the user has the brp_zoeken role in their overall roles
# BECAUSE they have the brp_zoeken role in PABC 'for no entity type at all'
# THEN the auth request succeeds
brp_zoeken if {
    brpZoeken.rol in user.overallRoles
}