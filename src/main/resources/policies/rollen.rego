#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.rol

behandelaar := {
    "rol": "behandelaar"
}

coordinator := {
    "rol": "coordinator"
}

recordmanager := {
    "rol": "recordmanager"
}

beheerder := {
    "rol": "beheerder"
}

raadpleger := {
    "rol": "raadpleger"
}

brpZoeken := {
    "rol": "brp_zoeken"
}

zaakspecifiekGeautoriseerd := {
    "rol": "zaakspecifiek_geautoriseerd"
}

systeemrolBehandelaarAlleZaaktypen := {
    "rol": "systeemrol_behandelaar_alle_zaaktypen"
}

# The application roles that grant read ('lezen') rights on zaken (for a certain zaaktype).
# ZAC also reads this set to restrict search results to the zaaktypen a user may read.
leesrollen := {raadpleger.rol, behandelaar.rol, coordinator.rol, recordmanager.rol, beheerder.rol}
