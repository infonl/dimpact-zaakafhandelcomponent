## ADDED Requirements

### Requirement: Saving or completing a taak returns the user to where the taak was opened from

After a taak is saved or completed successfully, ZAC SHALL navigate to the page before the taak in the navigation
trail. When the trail has no page before the taak, ZAC SHALL navigate to the dashboard. When the taak was opened
from a link in an e-mail, ZAC SHALL stay on the taak.

#### Scenario: Saving a taak opened from a zaak
- **GIVEN** a taak opened from its zaak
- **WHEN** the user saves the taak
- **THEN** ZAC shows the zaak

#### Scenario: Saving a taak opened from the takenwerkvoorraad
- **GIVEN** a taak opened from the takenwerkvoorraad
- **WHEN** the user saves the taak
- **THEN** ZAC shows the takenwerkvoorraad

#### Scenario: Saving a taak opened from a direct link or the search panel
- **GIVEN** a taak opened from a direct link or from the search panel
- **WHEN** the user saves the taak
- **THEN** ZAC shows the dashboard

#### Scenario: Saving a taak opened from an e-mail
- **GIVEN** a taak opened from a link in an e-mail
- **WHEN** the user saves the taak
- **THEN** ZAC stays on the taak

#### Scenario: Completing a taak navigates the same way as saving it
- **GIVEN** a taak opened from any page
- **WHEN** the user completes the taak
- **THEN** ZAC navigates as it does after saving that taak

### Requirement: The werklijst the user returns to shows the saved or completed taak

After a taak is saved or completed successfully, and before ZAC navigates away from the taak, ZAC SHALL mark its
cached copies of the werklijsten as stale: the takenwerkvoorraad, the taken and zoek cards on the dashboard, and
the taken of the zaak. A werklijst SHALL read its data again when it is shown next, instead of showing the copy
from before the taak was saved or completed.

#### Scenario: Completing a taak opened from the takenwerkvoorraad
- **GIVEN** the user opened a taak from the takenwerkvoorraad
- **WHEN** the user completes the taak
- **THEN** ZAC shows the takenwerkvoorraad without the completed taak

#### Scenario: Completing a taak opened from a zaak
- **GIVEN** the user opened a taak from its zaak
- **WHEN** the user completes the taak
- **THEN** ZAC shows the zaak with the taak as completed in its list of taken

#### Scenario: Saving a taak opened from the dashboard
- **GIVEN** the user opened a taak from a taken card on the dashboard
- **WHEN** the user saves the taak
- **THEN** ZAC shows the dashboard with the taak as it was saved
