## ADDED Requirements

### Requirement: Saving or completing a taak returns the user to where the taak was opened from

After a taak is saved or completed successfully, ZAC SHALL apply the first of these rules that matches:

1. When the taak was opened from a link in an e-mail, ZAC SHALL stay on the taak.
2. When the user visited a page before the taak in the navigation trail, ZAC SHALL navigate to that page.
3. Otherwise ZAC SHALL navigate to the dashboard. This includes a taak opened from a direct link, from the search
   panel or in a new browser tab: the trail then shows only the parent route of the taak, which the user did not
   visit. ZAC cannot tell a new tab from a direct link.

#### Scenario: Saving a taak opened from a zaak
- **GIVEN** a taak opened from its zaak
- **WHEN** the user saves the taak
- **THEN** ZAC navigates back to the zaak

#### Scenario: Saving a taak opened from the takenwerkvoorraad
- **GIVEN** a taak opened from the takenwerkvoorraad
- **WHEN** the user saves the taak
- **THEN** ZAC navigates back to the takenwerkvoorraad

#### Scenario: Saving a taak opened from a direct link or the search panel
- **GIVEN** a taak opened from a direct link or from the search panel
- **WHEN** the user saves the taak
- **THEN** ZAC navigates to the dashboard

#### Scenario: Saving a taak opened in a new tab from the takenwerkvoorraad
- **GIVEN** a taak opened in a new browser tab from the takenwerkvoorraad
- **WHEN** the user saves or completes the taak
- **THEN** ZAC navigates to the dashboard, as for a direct link

#### Scenario: Saving a taak opened from an e-mail
- **GIVEN** a taak opened from a link in an e-mail
- **WHEN** the user saves the taak
- **THEN** ZAC stays on the taak

#### Scenario: Completing a taak navigates the same way as saving it
- **GIVEN** a taak opened from any page
- **WHEN** the user completes the taak
- **THEN** ZAC navigates as it does after saving that taak

### Requirement: The werklijst the user returns to shows the saved or completed taak

Saving or completing a taak SHALL update the taak in the search index and make that change searchable before the
request returns, so the werklijsten that read from it show the saved values, such as the toelichting and the
fatale datum, and no longer show a completed taak. When converting the taak for the index fails, ZAC SHALL log a
warning and still report the save as successful; the werklijsten then show the previous values until the taak is
indexed again.

After a taak is saved or completed successfully, and before ZAC navigates away from the taak, ZAC SHALL mark its
cached copies of the werklijsten as stale: the takenwerkvoorraad, the taken and zoek cards on the dashboard, and
the taken of the zaak. A werklijst SHALL read its data again when it is shown next, instead of showing the copy
from before the taak was saved or completed.

#### Scenario: Completing a taak opened from the takenwerkvoorraad
- **GIVEN** the user opened a taak from the takenwerkvoorraad
- **WHEN** the user completes the taak
- **THEN** ZAC navigates back to the takenwerkvoorraad, which no longer lists the completed taak

#### Scenario: Completing a taak opened from a zaak
- **GIVEN** the user opened a taak from its zaak
- **WHEN** the user completes the taak
- **THEN** ZAC navigates back to the zaak, which lists the taak as completed

#### Scenario: Saving a taak opened from the dashboard
- **GIVEN** the user opened a taak from a taken card on the dashboard
- **WHEN** the user saves the taak
- **THEN** ZAC navigates back to the dashboard, which shows the taak as it was saved
