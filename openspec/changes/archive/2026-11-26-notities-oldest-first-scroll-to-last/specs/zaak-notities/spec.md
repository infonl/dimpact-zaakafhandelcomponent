## MODIFIED Requirements

### Requirement: Notes open in the side panel

Choosing *Notities* SHALL open the notes in the side panel on the right of the zaak view. The panel SHALL show a heading with the label and the number, a close button, the notes in the order they were added with the newest last, and a button *Sluiten*. Each note SHALL show the name of the employee, the date of the last change and the text.

#### Scenario: Open the notes
- **WHEN** the employee chooses *Notities* in the menu
- **THEN** the side panel opens with the heading *Notities* and the notes of the zaak, the oldest at the top and the newest at the bottom

#### Scenario: Notes of the same day
- **WHEN** a zaak has several notes that were added on the same day
- **THEN** the panel shows them in the order they were added, the newest at the bottom

#### Scenario: Changed note keeps its place
- **WHEN** an employee changes the text of a note
- **THEN** the note keeps its place in the list

#### Scenario: Close the notes
- **WHEN** the employee chooses the close button or *Sluiten* in the notes panel
- **THEN** the side panel closes

### Requirement: Adding, changing and deleting notes in the panel

The notes panel SHALL let an employee who may change notes add a note with the field *Voeg een notitie toe* and its send button, and change or delete their own notes that may be edited. An employee who may only read notes SHALL see the notes without these controls. A note SHALL hold at most 1000 characters.

#### Scenario: Add a note
- **WHEN** an employee who may change notes types a text and chooses the send button
- **THEN** the note is saved, shows at the bottom of the list, and the field is empty again

#### Scenario: New note and field in view
- **WHEN** an employee adds a note to a zaak with more notes than fit in the panel
- **THEN** the list scrolls to the bottom, so the new note and the field *Voeg een notitie toe* are in view

#### Scenario: Empty note
- **WHEN** the field *Voeg een notitie toe* is empty
- **THEN** the send button is disabled

#### Scenario: Change a note
- **WHEN** an employee who may change notes chooses the edit button of a note they may edit, changes the text and saves it
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

## ADDED Requirements

### Requirement: Only the notes scroll in the panel

The notes panel SHALL scroll only the list of notes. The heading with the close button and the button *Sluiten* SHALL stay in place, and the side panel itself SHALL NOT move.

#### Scenario: Long list of notes
- **WHEN** a zaak has more notes than fit in the panel
- **THEN** the list of notes scrolls, while the heading *Notities (n)* and the button *Sluiten* stay in view

#### Scenario: Panel stays in place after adding a note
- **WHEN** the employee adds a note
- **THEN** the heading *Notities (n)* stays fully in view and the side panel does not move up
