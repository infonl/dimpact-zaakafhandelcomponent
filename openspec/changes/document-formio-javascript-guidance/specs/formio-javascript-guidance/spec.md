## ADDED Requirements

### Requirement: BPMN handleiding warns that Form.io JavaScript executes as trusted code
The ZAC BPMN handleiding (`docs/manuals/bpmn-guide/README.md`) SHALL state that JavaScript used in Form.io `custom` component logic executes as arbitrary code in the browser of every user who opens the form, and MUST therefore be treated as trusted code rather than as data.

#### Scenario: Beheerder reads the Form.io JavaScript guidance
- **WHEN** a municipality beheerder opens the "Form.io form" section of the ZAC BPMN handleiding to build or upload a form
- **THEN** the handleiding contains a clearly identifiable section explaining that `custom` component JavaScript logic runs unrestricted in the browser and should only be added by someone who understands that risk

### Requirement: BPMN handleiding gives per-component-type JavaScript guidance
The ZAC BPMN handleiding SHALL document, per Form.io component type where script-like logic can be configured (calculated value, conditional/advanced logic, validation, and `custom` component logic), whether raw JavaScript or the JSONLogic alternative should be used.

#### Scenario: Beheerder configures conditional logic or custom validation
- **WHEN** a beheerder needs to add conditional (advanced logic) or custom validation behavior to a form component
- **THEN** the handleiding recommends using Form.io's JSONLogic option instead of raw JavaScript for these component types, because JSONLogic does not execute arbitrary code, and recommends Simple Conditions (no code at all) over Advanced Conditions where possible

#### Scenario: Beheerder configures a calculated value
- **WHEN** a beheerder needs to add a calculated value to a form component
- **THEN** the handleiding states that Form.io does not offer a JSONLogic option for calculated values, and instead recommends keeping the calculation a pure expression (only reading `data`/`row` and returning a value, with no DOM access, no `submit()` calls, and no network calls)

#### Scenario: Beheerder needs behavior JSONLogic cannot express
- **WHEN** a beheerder needs behavior that JSONLogic cannot express (e.g. DOM manipulation or custom submit handling in a `custom` component, as used for the "save and show last-saved timestamp" pattern)
- **THEN** the handleiding states that raw JavaScript in `custom` component logic is the highest-risk option and should only be used as a last resort, by someone who understands the code being added

### Requirement: BPMN handleiding links to authoritative JSONLogic references
The ZAC BPMN handleiding SHALL link to the official JSONLogic reference/playground and to Form.io's own documentation describing where JSON Logic can be used instead of JavaScript.

#### Scenario: Beheerder wants to learn JSONLogic syntax
- **WHEN** a beheerder reads the JavaScript guidance section and wants to write a JSONLogic rule instead of JavaScript
- **THEN** the handleiding provides a working link to https://jsonlogic.com/ (spec and interactive playground), a working link to https://jsonlogic.com/operations.html (operator reference), and a working link to https://help.form.io/form-building/logic-and-conditions (Form.io's documentation of Advanced Conditions, Logic triggers, and Custom Validation, including JSON Logic support)

### Requirement: BPMN handleiding provides recommended vs not-recommended JavaScript examples
The ZAC BPMN handleiding SHALL include concrete, side-by-side "not recommended" and "recommended" code examples illustrating the JavaScript guidance, so beheerders can compare their own form logic against a template.

#### Scenario: Beheerder compares a conditional/validation example
- **WHEN** a beheerder reads the examples for conditional display or custom validation
- **THEN** the handleiding shows a "not recommended" raw-JavaScript example and a "recommended" JSON Logic (or Simple Conditions) equivalent for the same behavior

#### Scenario: Beheerder compares the custom component logic example
- **WHEN** a beheerder reads the example for `custom` component logic
- **THEN** the handleiding shows the existing production "save button / last-saved timestamp" pattern (which manipulates `instance.root`, overrides `root.shouldValidate`, injects a `<style>` element, and force-submits/reloads the page) explicitly labeled as high-risk, together with a description of which parts of that pattern are the risky parts (DOM injection, disabling validation, forced submit) and what to keep in mind if similar logic cannot be avoided

### Requirement: Developer-facing bpmn.md points to the handleiding guidance
`docs/development/bpmn.md` SHALL link to the new Form.io JavaScript guidance section in the ZAC BPMN handleiding.

#### Scenario: Developer reads docs/development/bpmn.md
- **WHEN** a developer reads the "BPMN form.io forms" section of `docs/development/bpmn.md`
- **THEN** it contains a reference/link to the Form.io JavaScript guidance section in `docs/manuals/bpmn-guide/README.md`
