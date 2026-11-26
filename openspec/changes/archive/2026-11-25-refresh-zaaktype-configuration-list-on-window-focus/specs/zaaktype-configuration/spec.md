## MODIFIED Requirements

### Requirement: The list page shows the configurations as they are now

The Zaakafhandelparameters page SHALL read its list again each time a beheerder opens the page, and each time the
beheerder comes back to the browser window or tab while the page is open, so that it shows a zaaktype version for
which ZAC created a configuration in the meantime. While the list is read again, the page SHALL keep showing the
rows it read before.

#### Scenario: A new zaaktype version shows when the beheerder opens the list again
- **GIVEN** a beheerder who opened the Zaakafhandelparameters page
- **AND** a new version of a zaaktype was published in Open Zaak afterwards, and ZAC created its configuration
- **WHEN** the beheerder opens the page again
- **THEN** the page shows the new version with the procesmodel it took over, and its row links to the edit page of
  the new version

#### Scenario: A new zaaktype version shows when the beheerder comes back to the window or tab
- **GIVEN** a beheerder with the Zaakafhandelparameters page open in a browser window
- **AND** a new version of a zaaktype was published in Open Zaak in another window or tab, and ZAC created its
  configuration
- **WHEN** the beheerder comes back to the window or tab with the page
- **THEN** the page shows the new version with the procesmodel it took over
