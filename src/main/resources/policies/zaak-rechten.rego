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
import data.net.atos.zac.rol.systeemrolBehandelaarAlleZaaktypen
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
    "wijzigen_locatie": wijzigen_locatie
}

default zaaktype_allowed := false
zaaktype_allowed if {
    not user.zaaktypen
}
zaaktype_allowed if {
    zaak.zaaktype in user.zaaktypen
}

default lezen := false
lezen if {
    raadpleger.rol in user.rollen
    zaaktype_allowed
}
lezen if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default wijzigen := false
wijzigen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

wijzigen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
wijzigen if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default toekennen := false
toekennen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toekennen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toekennen if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default behandelen := false
behandelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}
behandelen if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default afbreken := false
afbreken if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}
afbreken if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default heropenen := false
heropenen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}

default bekijken_zaakdata := false
bekijken_zaakdata if {
    beheerder.rol in user.rollen
}

default wijzigen_doorlooptijd := false
wijzigen_doorlooptijd if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
wijzigen_doorlooptijd if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default verlengen := false
verlengen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
    not zaak.verlengd
}
verlengen if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default opschorten := false
opschorten if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.heropend
    not zaak.opgeschort
}
opschorten if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default hervatten := false
hervatten if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
}
hervatten if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default creeren_document := false
creeren_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
creeren_document if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default toevoegen_document := false
toevoegen_document if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_document if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toevoegen_document if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default koppelen := false
koppelen if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

koppelen if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
koppelen if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default versturen_email := false
versturen_email if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
versturen_email if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default versturen_ontvangstbevestiging := false
versturen_ontvangstbevestiging if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
versturen_ontvangstbevestiging if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default toevoegen_initiator_persoon := false
toevoegen_initiator_persoon if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_initiator_persoon if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toevoegen_initiator_persoon if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default toevoegen_initiator_bedrijf := false
toevoegen_initiator_bedrijf if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_initiator_bedrijf if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toevoegen_initiator_bedrijf if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default verwijderen_initiator := false
verwijderen_initiator if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

verwijderen_initiator if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
verwijderen_initiator if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default toevoegen_betrokkene_persoon := false
toevoegen_betrokkene_persoon if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_betrokkene_persoon if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toevoegen_betrokkene_persoon if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default toevoegen_betrokkene_bedrijf := false
toevoegen_betrokkene_bedrijf if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_betrokkene_bedrijf if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toevoegen_betrokkene_bedrijf if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default verwijderen_betrokkene := false
verwijderen_betrokkene if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

verwijderen_betrokkene if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
verwijderen_betrokkene if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default toevoegen_bag_object := false
toevoegen_bag_object if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}

toevoegen_bag_object if {
    recordmanager.rol in user.rollen
    zaaktype_allowed
}
toevoegen_bag_object if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default starten_taak := false
starten_taak if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
starten_taak if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default vastleggen_besluit := false
vastleggen_besluit if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
    not zaak.intake
    zaak.besloten
}
vastleggen_besluit if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default verlengen_doorlooptijd := false
verlengen_doorlooptijd if {
    behandelaar.rol in user.rollen
    zaaktype_allowed
    zaak.open
}
verlengen_doorlooptijd if {
    systeemrolBehandelaarAlleZaaktypen.rol in user.rollen
}

default wijzigen_locatie := false
wijzigen_locatie if {
    wijzigen
}

wijzigen_locatie if {
    wijzigen
    recordmanager.rol in user.rollen
}
