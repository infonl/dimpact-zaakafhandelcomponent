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
