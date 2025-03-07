#
# SPDX-FileCopyrightText: 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#
# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.werklijst

import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.recordmanager
import input.user

# Ensure user.rollen is iterable
user_rollen := {r | r := user.rollen[_]}

werklijst_rechten := {
    "inbox": inbox,
    "ontkoppelde_documenten_verwijderen": ontkoppelde_documenten_verwijderen,
    "inbox_productaanvragen_verwijderen": inbox_productaanvragen_verwijderen,
    "zaken_taken": zaken_taken,
    "zaken_taken_verdelen": zaken_taken_verdelen,
    "zaken_taken_exporteren": zaken_taken_exporteren
}

default inbox := false
inbox if {
    behandelaar.rol in user_rollen
}

default ontkoppelde_documenten_verwijderen := false
ontkoppelde_documenten_verwijderen if {
    recordmanager.rol in user_rollen
}

default inbox_productaanvragen_verwijderen := false
inbox_productaanvragen_verwijderen if {
    recordmanager.rol in user_rollen
}

default zaken_taken := false
zaken_taken if {
    behandelaar.rol in user_rollen
}

default zaken_taken_verdelen := false
zaken_taken_verdelen if {
    coordinator.rol in user_rollen
}

default zaken_taken_exporteren := false
zaken_taken_exporteren if {
    beheerder.rol in user_rollen
}
