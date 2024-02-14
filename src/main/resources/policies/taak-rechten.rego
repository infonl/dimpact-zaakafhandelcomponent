#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.taak

import future.keywords
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.recordmanager
import input.user
import input.taak

taak_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen,
    "toekennen": toekennen,
    "toevoegen_document": toevoegen_document
}

default zaaktype_allowed := false
zaaktype_allowed {
    not user.zaaktypen
}
zaaktype_allowed {
    taak.zaaktype in user.zaaktypen
}

default lezen := false
lezen {
    { behandelaar, recordmanager }[_].rol in user.rollen
    zaaktype_allowed == true
}

default wijzigen := false
wijzigen {
    { behandelaar, recordmanager }[_].rol in user.rollen
    zaaktype_allowed == true
}

default toekennen := false
toekennen {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
}

default toevoegen_document := false
toevoegen_document {
    behandelaar.rol in user.rollen
    zaaktype_allowed == true
    taak.zaak_open == true
    taak.open == true
}
toevoegen_document {
    recordmanager.rol in user.rollen
    zaaktype_allowed == true
    taak.open == true
}
