## Purpose

Keeps the catalogue data ZAC caches from Open Zaak in line with Open Zaak, without a manual action by a
beheerder.

## ADDED Requirements

### Requirement: A zaaktype notification refreshes all cached catalogue data

When Open Zaak sends a notification that a zaaktype was created or changed, ZAC SHALL discard all catalogue
data it caches from Open Zaak: zaaktypen, statustypen, resultaattypen, roltypen, eigenschappen,
informatieobjecttypen, the informatieobjecttypen per zaaktype, and besluittypen. The next read of any of these
SHALL fetch it from Open Zaak again.

#### Scenario: A besluittype is added to a zaaktype
- **GIVEN** ZAC has cached the besluittypen of zaaktype Z
- **WHEN** a beheerder adds a besluittype to Z in Open Zaak and Open Zaak sends the zaaktype notification
- **THEN** the next time ZAC lists the besluittypen of Z, the new besluittype is included

#### Scenario: An informatieobjecttype is linked to a zaaktype
- **GIVEN** ZAC has cached the informatieobjecttypen of zaaktype Z
- **WHEN** a beheerder links an informatieobjecttype to Z in Open Zaak and Open Zaak sends the zaaktype
  notification
- **THEN** the next time ZAC lists the informatieobjecttypen of Z, the linked informatieobjecttype is included

#### Scenario: A statustype is renamed
- **GIVEN** ZAC has cached the statustypen of zaaktype Z
- **WHEN** a beheerder renames a statustype of Z in Open Zaak and Open Zaak sends the zaaktype notification
- **THEN** the next time ZAC reads the statustypen of Z, it shows the new name
