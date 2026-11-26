## ADDED Requirements

### Requirement: A system mail template cannot be deleted

A system mail template is a mail template that ZAC marks as the default template of its mail type. The system SHALL
refuse to delete a system mail template, also when the request does not come from the admin screen, with a 400
response whose error code tells that a system mail template cannot be deleted. The system SHALL keep deleting a
normal mail template as before.

#### Scenario: A beheerder deletes a system mail template through the API
- **GIVEN** the system mail template of the mail type "Signalering taak op naam"
- **WHEN** a beheerder sends a delete request for that template to the REST API
- **THEN** the system answers 400 with the error code `msg.error.system.mailtemplate.cannot.be.deleted`
- **AND** the template still exists, unchanged

#### Scenario: A beheerder deletes a normal mail template
- **GIVEN** a normal mail template that no zaaktype configuration links
- **WHEN** a beheerder deletes it
- **THEN** the template no longer exists

### Requirement: A mail template that a zaaktype configuration links cannot be deleted

The system SHALL refuse to delete a mail template that the configuration of a zaaktype links, in its current or an
earlier version, with a 400 response whose error code tells that the mail template is linked to a zaaktype, instead
of the error of the database.

#### Scenario: A beheerder deletes a linked mail template through the API
- **GIVEN** a normal mail template that the configuration of a zaaktype links
- **WHEN** a beheerder sends a delete request for that template to the REST API
- **THEN** the system answers 400 with the error code `msg.error.mailtemplate.is.in.use.by.zaaktype-configuration`
- **AND** the template and the link still exist

#### Scenario: A beheerder deletes a mail template after removing its link
- **GIVEN** a normal mail template whose only link to a zaaktype configuration a beheerder has removed
- **WHEN** a beheerder deletes the template
- **THEN** the template no longer exists

### Requirement: A system mail template cannot be turned into a normal template or another mail type

The system SHALL refuse a change of a system mail template that sets it to a normal template or changes its mail
type, with a 400 response whose error code tells that a system mail template cannot be converted, and SHALL change
nothing of that template, also not the fields that the request would change as well.

#### Scenario: A beheerder turns a system mail template into a normal template
- **GIVEN** a system mail template
- **WHEN** a beheerder stores it with the system flag off and a changed subject
- **THEN** the system answers 400 with the error code `msg.error.system.mailtemplate.cannot.be.converted`
- **AND** the template is still a system mail template, with its previous subject

#### Scenario: A beheerder changes the mail type of a system mail template
- **GIVEN** the system mail template of the mail type "Signalering taak op naam"
- **WHEN** a beheerder stores it with the mail type "Zaak algemeen"
- **THEN** the system answers 400 with the error code `msg.error.system.mailtemplate.cannot.be.converted`
- **AND** the template keeps the mail type "Signalering taak op naam"

### Requirement: The content of a system mail template stays editable

The system SHALL store a change of the name, the subject or the body of a system mail template that keeps its mail
type and its system flag.

#### Scenario: A beheerder changes the content of a system mail template
- **GIVEN** a system mail template
- **WHEN** a beheerder stores it with a changed name, subject and body, and the same mail type and system flag
- **THEN** the template has the changed name, subject and body
- **AND** it is still the system mail template of its mail type

### Requirement: A mail template cannot become a system mail template through the API

The system SHALL refuse to create a mail template as a system mail template, and to turn a normal mail template into
a system mail template, with a 400 response whose error code tells that a system mail template cannot be converted,
so that each mail type keeps the single system mail template that ZAC created. The system SHALL keep creating and
changing normal mail templates as before.

#### Scenario: A beheerder creates a system mail template through the API
- **WHEN** a beheerder creates a mail template with the system flag on
- **THEN** the system answers 400 with the error code `msg.error.system.mailtemplate.cannot.be.converted`
- **AND** no mail template is created

#### Scenario: A beheerder turns a normal mail template into a system mail template
- **GIVEN** a normal mail template
- **WHEN** a beheerder stores it with the system flag on
- **THEN** the system answers 400 with the error code `msg.error.system.mailtemplate.cannot.be.converted`
- **AND** the template is still a normal mail template

#### Scenario: A beheerder creates and changes a normal mail template
- **WHEN** a beheerder creates a mail template with the system flag off, and then changes its name, subject and body
- **THEN** the template is stored with the changed name, subject and body, as a normal mail template
