#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.overig

import rego.v1

test_starten_zaak_with_behandelaar_role if {
    starten_zaak with input as {
        "user": {
            "rollen" : [ "behandelaar" ]
        }
    }
}

test_starten_zaak_with_unknown_role_fails if {
    not starten_zaak with input as {
        "user": {
            "rollen" : [ "basic" ]
        }
    }
}
