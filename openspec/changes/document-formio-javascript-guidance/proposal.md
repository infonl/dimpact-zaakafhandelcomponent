## Why

Form.io forms used in ZAC BPMN processes support embedding arbitrary client-side JavaScript (e.g. in `custom` component logic, calculated values, and conditional logic). Municipality beheerders who build and upload these forms currently have no documented guidance on whether this is safe, and if so, where and how it may be used. Without guidance, beheerders may unknowingly introduce security risks (arbitrary code execution in the browser of anyone opening the task) when authoring forms. This is a Jira-tracked requirement (PZ ticket, see conversation context) with the acceptance criterion that the ZAC BPMN handleiding makes clear whether and how JavaScript can be safely used.

## What Changes

- Add a new "JavaScript in Form.io formulieren" section to the ZAC BPMN handleiding (`docs/manuals/bpmn-guide/README.md`) aimed at municipality beheerders, covering:
  - That Form.io custom JavaScript logic executes arbitrary code in the browser of every user who opens the form, and should be treated as trusted code, not data.
  - Per-component-type guidance: prefer the JSONLogic option (no `eval`) over raw JavaScript for calculated values, conditional logic, and validation; raw JavaScript in `custom` component logic carries the highest risk and should only be used when JSONLogic cannot express the required behavior.
  - Web links to the official JSONLogic reference/playground and to Form.io's own documentation on where JSONLogic can be used instead of JavaScript.
  - Concrete "recommended" vs "not recommended" JavaScript/JSONLogic examples, so beheerders have a direct template to compare their own form logic against.
  - A short, practical recommendation for beheerders on when JavaScript use is (not) appropriate.
  - A forward-looking warning that ZAC may in the future prevent uploading forms containing JavaScript logic, while JSON Logic forms will always remain allowed — giving beheerders a concrete incentive to prefer JSON Logic now.
- Add a pointer from `docs/development/bpmn.md` to the new section in the handleiding, so developers land on the guidance too.
- Out of scope for this change: guidance aimed at ZAC developers/maintainers on preventing malicious JavaScript or malware from being uploaded into ZAC (e.g. upload-time scanning/warnings) — that is internal, developer-facing advice and will be addressed in a separate follow-up change.

## Capabilities

### New Capabilities
- `formio-javascript-guidance`: Documentation capability covering the guidance given to municipality beheerders on if/how JavaScript may be used in Form.io forms uploaded into ZAC.

### Modified Capabilities
(none — no existing spec's requirements change)

## Impact

- Affected files: `docs/manuals/bpmn-guide/README.md`, `docs/development/bpmn.md`.
- No code, API, or behavioral changes — documentation only.
