#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.overig

import rego.v1

import data.net.atos.zac.overig.zaaktype_allowed

test_zaaktype_allowed_with_mising_zaaktype if {
    not zaaktype_allowed with input.document.key as "value"
}
