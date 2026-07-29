## Context

Form.io forms in ZAC support several places where beheerders can embed JavaScript-like logic:
- `custom` component logic (raw JavaScript, executed via Form.io's `evaluate`/`new Function` mechanism in the browser)
- Calculated value expressions
- Conditional (advanced logic) expressions
- Custom validation logic

Form.io also offers **JSONLogic** as a declarative alternative to raw JavaScript for calculated values, conditional logic, and validation. JSONLogic expressions are evaluated without `eval`/`new Function` and cannot access the DOM, `window`, cookies, or perform arbitrary side effects — they can only compute a value from the submission data.

Today, `docs/manuals/bpmn-guide/README.md` (the ZAC BPMN handleiding used by municipality beheerders) documents form components and ZAC extensions but says nothing about the security implications of using JavaScript in a form definition. Beheerders currently copy patterns like the example in the Jira ticket (a `custom` component that manipulates `instance.root`, injects a `<style>` tag, and calls `root.submit()`) without knowing this is arbitrary code execution in the browser of anyone who opens that task.

This change only documents guidance for beheerders. It intentionally excludes developer-facing mitigations (e.g. upload-time scanning for JavaScript/malware, approval workflows) — those are tracked separately as internal follow-up work.

## Goals / Non-Goals

**Goals:**
- Make clear, in the ZAC BPMN handleiding, that Form.io JavaScript (`custom` component logic) executes arbitrary code in the browser of every user who opens the form, and must be treated as trusted code, not data.
- Give beheerders a concrete, per-component-type answer to "can I use JavaScript here, and should I": prefer JSONLogic for calculated values / conditional logic / validation; treat raw `custom` JavaScript as the highest-risk, last-resort option.
- Keep the guidance short and actionable — beheerders configuring a form need a quick answer, not a security essay.

**Non-Goals:**
- Documenting or implementing upload-time detection/warnings for JavaScript in form definitions (separate, developer-facing follow-up).
- Documenting malware/virus scanning of uploaded files (separate, developer-facing follow-up, and a different threat — file content, not form logic).
- Changing any runtime behavior of the Form.io renderer or the form upload feature. This change is documentation-only.

## Decisions

- **Where the guidance lives**: add a new section to `docs/manuals/bpmn-guide/README.md`, right after the existing "Form.io form" / "ZAC extensions" sections, rather than creating a new standalone document. Rationale: this is the document beheerders already use when building/uploading forms (per AC1 of the source ticket, "De ZAC BPMN handleiding maakt dit duidelijk"); a separate doc would likely be missed.
- **`docs/development/bpmn.md`**: add a one-line pointer to the new section, consistent with how it already links to the BPMN guide. This file is developer-facing and not the primary target, so it only gets a pointer, not the guidance itself.
- **Framing as "prefer JSONLogic, avoid raw JavaScript where possible"** rather than an outright ban. Rationale: ZAC already ships forms using `custom` JavaScript (e.g. the save-button/timestamp example in the ticket background) and some behaviors genuinely require it (DOM manipulation, `root.submit()` orchestration) that JSONLogic cannot express. An outright ban would be inaccurate and unenforceable from documentation alone; a risk-tiered recommendation is both honest and actionable.
- **Leave out AC3 (malware/injection-prevention advice for developers)**: explicitly scoped out per the request — that guidance is internal, developer/maintainer-facing (upload-time scanning, review process) and belongs in a separate change once designed.
- **Reference links**: link to the vendor-neutral, official sources rather than a blog/tutorial, so the guidance stays authoritative and doesn't need re-verifying against a third party's interpretation:
  - [jsonlogic.com](https://jsonlogic.com/) — the JSONLogic spec itself, with an interactive playground.
  - [jsonlogic.com/operations.html](https://jsonlogic.com/operations.html) — full operator reference.
  - [help.form.io/form-building/logic-and-conditions](https://help.form.io/form-building/logic-and-conditions) — Form.io's own docs on Advanced Conditions, Logic triggers, and Custom Validation, which explicitly document JSON Logic as an alternative to JavaScript and warn that "JavaScript conditions require secure evaluation in order to execute. This introduces an amount of overhead that can degrade form performance", recommending Simple Conditions as best practice first.
- **Concrete examples, framed as "not recommended" vs "recommended" rather than "forbidden" vs "allowed"**: use the ticket's own real-world example (the save-button `custom` logic that sets `root.shouldValidate`, injects a `<style>` tag, and force-submits/reloads) as the "not recommended" illustration, since it is already in production and beheerders will recognize it. Pair each "not recommended" example with a safer alternative:
  - Conditional display: raw JavaScript in Advanced Conditions → prefer Simple Conditions (no code) or JSON Logic.
  - Custom validation: raw JavaScript reading/writing outside the field being validated → prefer a JSON Logic validation rule scoped to `data`.
  - Custom component logic that reaches into `instance.root`, mutates global validation behavior (`root.shouldValidate`), injects DOM/`<style>` nodes, or force-submits the form → flagged as high-risk regardless of alternative, since JSONLogic cannot express this and no safe drop-in replacement exists yet; beheerders are advised to keep such logic minimal, side-effect-free where possible, and to have it reviewed by someone who understands JavaScript before uploading the form.
  - Calculated values: Form.io does not offer a JSONLogic option here (confirmed against the official docs), so the guidance for this component type is to keep the calculation a pure expression (only reads `data`/`row` and returns a value) with no DOM access, no `submit()`, no network calls — rather than to switch to JSONLogic.

## Risks / Trade-offs

- [Documentation-only change has no enforcement mechanism — a beheerder can still ignore the guidance and paste risky JavaScript] → Mitigated by the planned follow-up story (upload-time warning) referenced in the ticket; out of scope here but the doc should not overstate itself as a control.
- [Guidance could go stale if Form.io's JSONLogic support or component set changes] → Keep the guidance principle-based (executes in browser / no eval vs. eval) rather than tied to specific Form.io version behavior, so it degrades gracefully.

## Open Questions

None — scope is confirmed as beheerder-facing guidance only, AC3 explicitly deferred.
