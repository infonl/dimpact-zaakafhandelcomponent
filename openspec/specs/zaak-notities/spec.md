# zaak-notities Specification

## Purpose
Defines where an employee finds the notes of a zaak in the zaak view, how the number of notes is shown, and who can
read and change them.

## Requirements

### Requirement: Notes action in the zaak menu

The zaak view SHALL show the action *Notities* in the *Zaak* section of the menu on the left, after the document
actions and before *Zaakdata*, when the employee may read or change notes. The zaak view SHALL NOT show a separate
notes button outside the menu.

#### Scenario: Employee with the right to read notes
- **WHEN** an employee who may read notes opens a zaak
- **THEN** the *Zaak* section of the menu shows the action *Notities*

#### Scenario: Employee with only the right to change notes
- **WHEN** an employee who may change notes but not read them opens a zaak
- **THEN** the *Zaak* section of the menu shows the action *Notities*

#### Scenario: Employee without rights on notes
- **WHEN** an employee who may neither read nor change notes opens a zaak
- **THEN** the menu does not show the action *Notities*

#### Scenario: No floating notes button
- **WHEN** an employee opens a zaak
- **THEN** the zaak view shows no notes button at the bottom right of the screen

### Requirement: Number of notes as plain text behind the label

The action *Notities* and the heading of the notes panel SHALL show the number of notes of the zaak behind the
label, in brackets, as plain text in the colour of the label. When the zaak has no notes, they SHALL show the label
without a number. The number SHALL NOT be shown as a coloured badge.

#### Scenario: Zaak with notes
- **WHEN** a zaak has 2 notes
- **THEN** the menu action reads *Notities (2)*

#### Scenario: Zaak without notes
- **WHEN** a zaak has no notes
- **THEN** the menu action reads *Notities*

#### Scenario: Collapsed menu
- **WHEN** the menu shows icons only and the employee points at the notes icon
- **THEN** the tooltip reads *Notities (2)* for a zaak with 2 notes

### Requirement: Number of notes stays current

The number of notes in the menu SHALL follow the notes of the zaak, also while the notes panel is closed.

#### Scenario: Employee adds a note
- **WHEN** the employee adds a note to a zaak with 2 notes
- **THEN** the menu action and the panel heading read *Notities (3)*

#### Scenario: Employee deletes a note
- **WHEN** the employee deletes a note of a zaak with 2 notes
- **THEN** the menu action and the panel heading read *Notities (1)*

#### Scenario: Another employee changes the notes
- **WHEN** another employee adds a note to the zaak while the notes panel is closed
- **THEN** the number in the menu action goes up by one without a page reload

### Requirement: Notes open in the side panel

Choosing *Notities* SHALL open the notes in the side panel on the right of the zaak view. The panel SHALL show a
heading with the label and the number, a close button, the notes with the newest change first, and a button
*Sluiten*. Each note SHALL show the name of the employee, the date of the last change and the text.

#### Scenario: Open the notes
- **WHEN** the employee chooses *Notities* in the menu
- **THEN** the side panel opens with the heading *Notities* and the notes of the zaak, newest change first

#### Scenario: Close the notes
- **WHEN** the employee chooses the close button or *Sluiten* in the notes panel
- **THEN** the side panel closes

### Requirement: Adding, changing and deleting notes in the panel

The notes panel SHALL let an employee who may change notes add a note with the field *Voeg een notitie toe* and its
send button, and change or delete their own notes that may be edited. An employee who may only read notes SHALL see
the notes without these controls. A note SHALL hold at most 1000 characters.

#### Scenario: Add a note
- **WHEN** an employee who may change notes types a text and chooses the send button
- **THEN** the note is saved, shows at the top of the list, and the field is empty again

#### Scenario: Empty note
- **WHEN** the field *Voeg een notitie toe* is empty
- **THEN** the send button is disabled

#### Scenario: Change a note
- **WHEN** an employee who may change notes chooses the edit button of a note they may edit, changes the text and
  saves it
- **THEN** the note shows the new text

#### Scenario: Cancel a change
- **WHEN** the employee chooses cancel while changing a note
- **THEN** the note keeps its old text and the panel leaves edit mode

#### Scenario: Delete a note
- **WHEN** an employee who may change notes chooses the delete button of a note they may edit
- **THEN** the note is removed from the list

#### Scenario: Read-only employee
- **WHEN** an employee who may only read notes opens the notes panel
- **THEN** the panel shows the notes without the field *Voeg een notitie toe* and without edit and delete buttons
