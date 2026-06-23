## ADDED Requirements

### Requirement: GebruikersvoorkeurenRESTService lists zoekopdrachten for the logged-in user
The service SHALL delegate to `GebruikersvoorkeurenService.listZoekopdrachten` with the given `Werklijst` and the current user's ID, and return the converted results.

#### Scenario: List zoekopdrachten
- **WHEN** `listZoekopdrachten` is called with a `Werklijst`
- **THEN** `GebruikersvoorkeurenService.listZoekopdrachten` is called with parameters containing the werklijst and logged-in user ID
- **THEN** the converted REST zoekopdrachten list is returned

### Requirement: GebruikersvoorkeurenRESTService deletes a zoekopdracht by ID
The service SHALL delegate deletion to `GebruikersvoorkeurenService.deleteZoekopdracht`.

#### Scenario: Delete zoekopdracht
- **WHEN** `deleteZoekopdracht` is called with an ID
- **THEN** `GebruikersvoorkeurenService.deleteZoekopdracht` is called with that ID

### Requirement: GebruikersvoorkeurenRESTService creates or updates a zoekopdracht
The service SHALL convert the REST input, delegate to the service, and return the converted result.

#### Scenario: Create or update zoekopdracht
- **WHEN** `createOrUpdateZoekopdracht` is called with a `RESTZoekopdracht`
- **THEN** `GebruikersvoorkeurenService.createZoekopdracht` is called
- **THEN** the saved zoekopdracht is converted and returned

### Requirement: GebruikersvoorkeurenRESTService reads tabel-gegevens
The service SHALL read `TabelInstellingen` for the given werklijst and current user, and combine them with werklijst rights from `PolicyService`.

#### Scenario: Read tabel-gegevens
- **WHEN** `readTabelGegevens` is called with a `Werklijst`
- **THEN** `GebruikersvoorkeurenService.readTabelInstellingen` is called with the werklijst and logged-in user ID
- **THEN** `PolicyService.readWerklijstRechten` is called
- **THEN** a `RESTTabelGegevens` is returned with `aantalPerPagina` from tabel instellingen

### Requirement: GebruikersvoorkeurenRESTService updates aantal items per pagina within allowed bounds
When the given `aantal` is within `[AANTAL_PER_PAGINA_MIN, AANTAL_PER_PAGINA_MAX]`, the service SHALL save the new setting. When out of bounds, no update SHALL be performed.

#### Scenario: Aantal within bounds
- **WHEN** `updateAantalItemsPerPagina` is called with a valid `aantal`
- **THEN** `GebruikersvoorkeurenService.updateTabelInstellingen` is called

#### Scenario: Aantal out of bounds
- **WHEN** `updateAantalItemsPerPagina` is called with an `aantal` exceeding the maximum
- **THEN** `GebruikersvoorkeurenService.updateTabelInstellingen` is NOT called

### Requirement: GebruikersvoorkeurenRESTService manages dashboard cards
The service SHALL support listing, adding, updating, and deleting dashboard cards by delegating to `GebruikersvoorkeurenService`.

#### Scenario: List dashboard cards
- **WHEN** `listDashboardCards` is called
- **THEN** `GebruikersvoorkeurenService.listDashboardCards` is called with the logged-in user ID
- **THEN** the converted list is returned
