#
# SPDX-FileCopyrightText: 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

# When updating this file, please make sure to also update the policy documentation
# in ~/docs/solution-architecture/accessControlPolicies.md
#
package net.atos.zac.zaak

import data.net.atos.zac.rol.beheerder
import data.net.atos.zac.rol.behandelaar
import data.net.atos.zac.rol.coordinator
import data.net.atos.zac.rol.raadpleger
import data.net.atos.zac.rol.recordmanager
import data.net.atos.zac.rol.zaakspecifiekGeautoriseerd
import input.zaak
import input.user

zaak_rechten := {
    "lezen": lezen,
    "wijzigen": wijzigen,
    "toekennen": toekennen,
    "behandelen": behandelen,
    "afbreken": afbreken,
    "heropenen": heropenen,
    "bekijken_zaakdata": bekijken_zaakdata,
    "wijzigen_doorlooptijd": wijzigen_doorlooptijd,
    "verlengen": verlengen,
    "opschorten": opschorten,
    "hervatten": hervatten,
    "creeren_document": creeren_document,
    "toevoegen_document": toevoegen_document,
    "koppelen": koppelen,
    "versturen_email": versturen_email,
    "versturen_ontvangstbevestiging": versturen_ontvangstbevestiging,
    "toevoegen_initiator_persoon": toevoegen_initiator_persoon,
    "toevoegen_initiator_bedrijf": toevoegen_initiator_bedrijf,
    "verwijderen_initiator": verwijderen_initiator,
    "toevoegen_betrokkene_persoon": toevoegen_betrokkene_persoon,
    "toevoegen_betrokkene_bedrijf": toevoegen_betrokkene_bedrijf,
    "verwijderen_betrokkene": verwijderen_betrokkene,
    "toevoegen_bag_object": toevoegen_bag_object,
    "starten_taak": starten_taak,
    "vastleggen_besluit": vastleggen_besluit,
    "verlengen_doorlooptijd": verlengen_doorlooptijd,
    "wijzigen_locatie": wijzigen_locatie,
    "brondatum_zetten": brondatum_zetten
}

default zaaktype_allowed := false
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    zaak.zaaktype in user.zaaktypen
}

# zaak_allowed guards access to a zaakspecifiek geautoriseerde zaak: unrestricted for a not zaakspecifiek geautoriseerde
# zaak, otherwise only for a user holding the zaakspecifiek_geautoriseerd application role.
default zaak_allowed := false
zaak_allowed if {
    not zaak.zaakspecifiekGeautoriseerd
}
zaak_allowed if {
    zaakspecifiekGeautoriseerd.rol in user.rollen
}

default lezen := false
lezen if {
    zaaktype_allowed
    zaak_allowed
    some role in {raadpleger, behandelaar, coordinator}
    role.rol in user.rollen
}
lezen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default wijzigen := false
wijzigen if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
wijzigen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toekennen := false
toekennen if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
toekennen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default behandelen := false
behandelen if {
    zaaktype_allowed
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
behandelen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default afbreken := false
afbreken if {
    zaaktype_allowed
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
afbreken if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default heropenen := false
heropenen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default bekijken_zaakdata := false
bekijken_zaakdata if {
    beheerder.rol in user.rollen
}

default wijzigen_doorlooptijd := false
wijzigen_doorlooptijd if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
wijzigen_doorlooptijd if {
    zaaktype_allowed
    zaak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default verlengen := false
verlengen if {
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
    not zaak.verlengd
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
verlengen if {
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
    not zaak.verlengd
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default opschorten := false
opschorten if {
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
opschorten if {
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default hervatten := false
hervatten if {
    zaaktype_allowed
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
hervatten if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default creeren_document := false
creeren_document if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
creeren_document if {
    zaaktype_allowed
    zaak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_document := false
toevoegen_document if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
toevoegen_document if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default koppelen := false
koppelen if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
koppelen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default versturen_email := false
versturen_email if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
versturen_email if {
    zaaktype_allowed
    zaak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default versturen_ontvangstbevestiging := false
versturen_ontvangstbevestiging if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
versturen_ontvangstbevestiging if {
    zaaktype_allowed
    zaak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_initiator_persoon := false
toevoegen_initiator_persoon if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
toevoegen_initiator_persoon if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_initiator_bedrijf := false
toevoegen_initiator_bedrijf if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
toevoegen_initiator_bedrijf if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default verwijderen_initiator := false
verwijderen_initiator if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
verwijderen_initiator if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_betrokkene_persoon := false
toevoegen_betrokkene_persoon if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
toevoegen_betrokkene_persoon if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_betrokkene_bedrijf := false
toevoegen_betrokkene_bedrijf if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
toevoegen_betrokkene_bedrijf if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default verwijderen_betrokkene := false
verwijderen_betrokkene if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
verwijderen_betrokkene if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default toevoegen_bag_object := false
toevoegen_bag_object if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
toevoegen_bag_object if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default starten_taak := false
starten_taak if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
starten_taak if {
    zaaktype_allowed
    zaak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default vastleggen_besluit := false
vastleggen_besluit if {
    zaaktype_allowed
    zaak.open
    not zaak.intake
    zaak.besloten
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
vastleggen_besluit if {
    zaaktype_allowed
    zaak.open
    not zaak.intake
    zaak.besloten
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default verlengen_doorlooptijd := false
verlengen_doorlooptijd if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
verlengen_doorlooptijd if {
    zaaktype_allowed
    zaak.open
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}

default wijzigen_locatie := false
wijzigen_locatie if {
    wijzigen
}

wijzigen_locatie if {
    wijzigen
    recordmanager.rol in user.rollen
}

default brondatum_zetten := false
brondatum_zetten if {
    not zaak.open
    not zaak.brondatumBepaald
    recordmanager.rol in user.rollen
}
