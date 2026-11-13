## ADDED Requirements

### Requirement: Deleting a document returns the user to where the document was opened from

After a document is deleted successfully, ZAC SHALL apply the first of these rules that matches:

1. When the user visited a page before the document in the navigation trail, ZAC SHALL navigate back to that page.
2. Otherwise, when the document belonged to a zaak, ZAC SHALL navigate to that zaak.
3. Otherwise ZAC SHALL navigate to the ontkoppelde documenten.

A document opened from a direct link or from the search panel has no visited page before it: its trail shows only
the parent route of the document, which the user did not visit. The browser history keeps the trail with its page,
so the visited page is kept when the user reloads the document or switches to another version of it.

#### Scenario: Deleting a document opened from the inbox documenten
- **GIVEN** a document opened from the inbox documenten
- **WHEN** the user deletes the document
- **THEN** ZAC navigates back to the inbox documenten

#### Scenario: Deleting a document opened from the inbox productaanvragen
- **GIVEN** a document opened from the inbox productaanvragen
- **WHEN** the user deletes the document
- **THEN** ZAC navigates back to the inbox productaanvragen

#### Scenario: Deleting another version of a document opened from the inbox documenten
- **GIVEN** a document opened from the inbox documenten
- **AND** the user reloaded the document and switched to an earlier version of it
- **WHEN** the user deletes the document
- **THEN** ZAC navigates back to the inbox documenten

#### Scenario: Deleting a document of a zaak opened from a direct link or the search panel
- **GIVEN** a document of a zaak, opened from a direct link or from the search panel
- **WHEN** the user deletes the document
- **THEN** ZAC navigates to the zaak

#### Scenario: Deleting a document without a zaak opened from a direct link or the search panel
- **GIVEN** a document without a zaak, opened from a direct link or from the search panel
- **WHEN** the user deletes the document
- **THEN** ZAC navigates to the ontkoppelde documenten
