## 1. BPMN handleiding: JavaScript guidance section

- [x] 1.1 Add a new "JavaScript in Form.io formulieren" section to `docs/manuals/bpmn-guide/README.md` (after the existing "ZAC extensions" section), explaining that `custom` component JavaScript logic executes as unrestricted code in the browser of every user opening the form and must be treated as trusted code, not data.
- [x] 1.2 In that section, document per-component-type guidance: recommend Form.io's JSONLogic option (and Simple Conditions where applicable) over raw JavaScript for conditional (advanced logic) and validation components; note that calculated values have no JSONLogic option and should instead be kept as pure, side-effect-free expressions.
- [x] 1.3 In that section, state that raw JavaScript in `custom` component logic is the highest-risk option and should only be used when JSONLogic cannot express the required behavior (e.g. DOM manipulation, custom submit handling), by someone who understands the code being added.
- [x] 1.4 Add links to https://jsonlogic.com/ (spec + playground), https://jsonlogic.com/operations.html (operator reference), and https://help.form.io/form-building/logic-and-conditions (Form.io's own JSON Logic / Advanced Conditions / Custom Validation docs).
- [x] 1.5 Add a "not recommended" vs "recommended" example for conditional display or custom validation (raw JavaScript vs. JSON Logic/Simple Conditions for the same behavior).
- [x] 1.6 Add a fabricated but realistic "not recommended" `custom`-logic illustration (not framed as an existing production form) — e.g. a `custom` component that calls an external API directly from the browser, disables form validation, and force-submits the form — explicitly calling out which parts are risky (renderer-internal state mutation, disabling validation, embedded credentials/direct external API calls, forced submit) and what to keep in mind if similar logic can't be avoided.
- [x] 1.7 Add a warning that ZAC may in the future prevent uploading Form.io forms containing JavaScript logic, and that forms using JSON Logic will always remain allowed.

## 2. Developer-facing pointer

- [x] 2.1 Add a link from the "BPMN form.io forms" section of `docs/development/bpmn.md` to the new "JavaScript in Form.io formulieren" section in `docs/manuals/bpmn-guide/README.md`.

## 3. Review

- [x] 3.1 Proofread both updated documents for consistency with existing tone/structure of the handleiding (Dutch section heading, English body text, matching the rest of the document).
- [x] 3.2 Confirm no developer-facing malware/upload-scanning advice (AC3 from the source ticket) was included — that remains out of scope for this change.
