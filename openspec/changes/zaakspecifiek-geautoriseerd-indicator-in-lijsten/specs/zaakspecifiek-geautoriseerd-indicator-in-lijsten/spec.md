## ADDED Requirements

### Requirement: The lock icon is shown directly in front of the zaaknummer of a zaakspecifiek geautoriseerde zaak
Wherever this capability lists a place, the system SHALL show a lock icon directly in front of the zaaknummer, in the
same row, when that zaak is zaakspecifiek geautoriseerd. The lock icon SHALL be the same in every place as the one
on the zaakdetailpagina, including its title text. When the zaak is not zaakspecifiek geautoriseerd, the zaaknummer
SHALL be displayed exactly as before this capability existed, with no lock icon.

#### Scenario: Zaaknummer of a zaakspecifiek geautoriseerde zaak
- **WHEN** a place listed by this capability shows the zaaknummer of a zaak that is zaakspecifiek geautoriseerd
- **THEN** a lock icon is shown directly in front of that zaaknummer, identical to the lock icon on the
  zaakdetailpagina

#### Scenario: Zaaknummer of a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** a place listed by this capability shows the zaaknummer of a zaak that is not zaakspecifiek geautoriseerd
- **THEN** no lock icon is shown in front of that zaaknummer, unchanged from current behaviour

### Requirement: Zoekresultaten show the lock icon
The zoekresultaten of the general search SHALL show the lock icon in front of the zaaknummer of a zaak result, and in
front of the *hoort bij zaak* zaaknummer of a taak result and of a document result, when that zaak is zaakspecifiek
geautoriseerd.

#### Scenario: Zaak result
- **WHEN** a user searches and a result is a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of its zaaknummer

#### Scenario: Taak result
- **WHEN** a user searches and a result is a taak that belongs to a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of the zaaknummer the taak belongs to

#### Scenario: Document result
- **WHEN** a user searches and a result is a document that belongs to a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of the zaaknummer the document belongs to

### Requirement: Werklijsten show the lock icon
The zaken werkvoorraad, mijn zaken and afgehandelde zaken werklijsten SHALL show the lock icon in front of the
zaaknummer of each zaakspecifiek geautoriseerde zaak. The taken werkvoorraad and mijn taken werklijsten SHALL show
it in front of the zaaknummer of each taak whose zaak is zaakspecifiek geautoriseerd.

#### Scenario: Zaak in a zaken werklijst
- **WHEN** a user opens the zaken werkvoorraad, mijn zaken or afgehandelde zaken and a row is a zaakspecifiek
  geautoriseerde zaak
- **THEN** the lock icon is shown in front of the zaaknummer in that row

#### Scenario: Taak in a taken werklijst
- **WHEN** a user opens the taken werkvoorraad or mijn taken and a row is a taak of a zaakspecifiek geautoriseerde
  zaak
- **THEN** the lock icon is shown in front of the zaaknummer in that row

### Requirement: Dashboard cards show the lock icon
Every dashboard card that shows a zaaknummer SHALL show the lock icon in front of the zaaknummer of a zaakspecifiek
geautoriseerde zaak. This covers the cards that list zaken (including *zaken op mijn naam*, *mijn binnenkort
verlopende zaken*, *zaken op mijn naam met recent toegevoegde documenten* and zaak zoekopdracht cards) and the cards
that list taken (including *taken op mijn naam* and taak zoekopdracht cards).

#### Scenario: Zaak on a dashboard card
- **WHEN** a dashboard card lists a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of its zaaknummer

#### Scenario: Taak on a dashboard card
- **WHEN** a dashboard card lists a taak of a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of the zaaknummer the taak belongs to

### Requirement: Taak and document detailpagina's show the lock icon
The zaak card on the taak detailpagina, and each zaak card on the document detailpagina, SHALL show the lock icon in
front of the zaaknummer when that zaak is zaakspecifiek geautoriseerd.

#### Scenario: Taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user opens the detailpagina of a taak whose zaak is zaakspecifiek geautoriseerd
- **THEN** the zaak card shows the lock icon in front of the zaaknummer

#### Scenario: Document linked to a zaakspecifiek geautoriseerde zaak
- **WHEN** a user opens the detailpagina of a document that is linked to a zaakspecifiek geautoriseerde zaak
- **THEN** the zaak card of that zaak shows the lock icon in front of the zaaknummer

### Requirement: Persoon, bedrijf and BAG pages show the lock icon
The zaken tables on the persoon, bedrijf and BAG-object pages SHALL show the lock icon in front of the zaaknummer of
each zaakspecifiek geautoriseerde zaak.

#### Scenario: Zaak in the zaken table of a persoon, bedrijf or BAG-object
- **WHEN** a user opens a persoon, bedrijf or BAG-object page whose zaken table lists a zaakspecifiek geautoriseerde
  zaak
- **THEN** the lock icon is shown in front of the zaaknummer in that row

### Requirement: Zaak koppelen results show the lock icon
The result list of the zaak koppelen screen, and of the screen that links a document to a zaak, SHALL show the lock
icon in front of the zaaknummer of each zaakspecifiek geautoriseerde zaak.

#### Scenario: Zaakspecifiek geautoriseerde zaak in the zaak koppelen results
- **WHEN** a user searches for a zaak on the zaak koppelen screen and a result is a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of its zaaknummer

#### Scenario: Zaakspecifiek geautoriseerde zaak in the document koppelen results
- **WHEN** a user searches for a zaak to link a document to and a result is a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of its zaaknummer

### Requirement: Zaakdetailpagina lists show the lock icon
On the zaakdetailpagina, the gerelateerde zaken tab SHALL show the lock icon in front of the zaaknummer of each
zaakspecifiek geautoriseerde zaak, and the zaak column of the documenten table SHALL show it in front of the
zaaknummer of each document whose zaak is zaakspecifiek geautoriseerd.

#### Scenario: Zaakspecifiek geautoriseerde zaak in the gerelateerde zaken tab
- **WHEN** a user opens the gerelateerde zaken tab of a zaak and a related zaak is zaakspecifiek geautoriseerd
- **THEN** the lock icon is shown in front of the zaaknummer of that related zaak

#### Scenario: Document of a zaakspecifiek geautoriseerde zaak in the documenten table
- **WHEN** the documenten table on the zaakdetailpagina lists a document of a zaakspecifiek geautoriseerde zaak
- **THEN** the lock icon is shown in front of the zaaknummer in the zaak column of that row

### Requirement: Ontkoppelde documenten list shows the lock icon
The ontkoppelde documenten list SHALL show the lock icon in front of the zaaknummer in the zaak column when the zaak
the document was unlinked from is zaakspecifiek geautoriseerd. When that zaak no longer exists, no lock icon SHALL be
shown.

#### Scenario: Document unlinked from a zaakspecifiek geautoriseerde zaak
- **WHEN** a user opens the ontkoppelde documenten list and a row's zaak is zaakspecifiek geautoriseerd
- **THEN** the lock icon is shown in front of the zaaknummer in that row

#### Scenario: Document unlinked from a zaak that no longer exists
- **WHEN** a user opens the ontkoppelde documenten list and a row's zaak no longer exists
- **THEN** no lock icon is shown in front of the zaaknummer in that row

### Requirement: REST responses behind these places expose whether the zaak is zaakspecifiek geautoriseerd
`RestTaakZoekObject`, `RestDocumentZoekObject`, `RestZaakKoppelenZoekObject`, `RestZaakOverzicht`,
`RestSignaleringTaskSummary`, `RestZaakInformatieobject`, `RestGerelateerdeZaak`,
`RestGekoppeldeZaakEnkelvoudigInformatieObject` and `RestDetachedDocument` SHALL each include a boolean field
`isZaakspecifiekGeautoriseerd`. It SHALL be `true` when the zaak the object is or belongs to is zaakspecifiek
geautoriseerd, determined in the same way as `RestZaak.isZaakspecifiekGeautoriseerd`, and `false` otherwise.

#### Scenario: Object of or belonging to a zaakspecifiek geautoriseerde zaak
- **WHEN** one of these REST objects is returned for a zaak, or for a taak or document of a zaak, that is
  zaakspecifiek geautoriseerd (for `RestDetachedDocument`: the zaak the document was unlinked from)
- **THEN** its `isZaakspecifiekGeautoriseerd` is `true`

#### Scenario: Object of or belonging to a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** one of these REST objects is returned for a zaak, or for a taak or document of a zaak, that is not
  zaakspecifiek geautoriseerd
- **THEN** its `isZaakspecifiekGeautoriseerd` is `false`
