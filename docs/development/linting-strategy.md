# Linting strategy

TypeScript and Angular templates are checked with one configuration, run the same way locally
and in CI. There is no "changed files only" mode for them and no way to opt out of a check for
a single pull request: what fails on your machine fails in the pipeline, and the other way
round.

## What runs

| Check                  | Command                              | Configuration        |
| ---------------------- | ------------------------------------ | -------------------- |
| ESLint                 | `npm run lint` (in `src/main/app`)   | `.eslintrc.js`       |
| TypeScript + templates | `npm run build` (in `src/main/app`)  | `tsconfig.app.json`  |
| TypeScript in specs    | `npm test` (in `src/main/app`)       | `tsconfig.spec.json` |

`./gradlew build` runs all three, so the pipeline executes the same commands a developer runs.

## TypeScript settings

`tsconfig.json` turns on `strict` and `strictTemplates`, and both `tsconfig.app.json` and
`tsconfig.spec.json` inherit them without overriding. A type error in a component, a spec or a
template therefore fails the build wherever it is found.

`strictTemplates` also reports guards that guard against nothing — an optional chain or a
nullish coalesce whose left side can never be absent. Those are warnings rather than errors;
remove the guard rather than leaving it in, because a guard the compiler knows is dead tells
the next reader something untrue about the value.

## Testing Library rules

`.eslintrc.js` prefers Testing Library queries over reaching into the DOM through Angular, and
`fromPartial` over casting an object literal through `unknown`. Older specs still break these
rules, so they are warnings project-wide and a whole-project `npm run lint` stays green on them.

They are **errors on every spec file a pull request touches**, which is what
`.eslintrc.strict-specs.js` is for: a spec you edit has to meet the standard before it merges.
The "Lint Changed Specs" workflow runs it on the changed specs; reproduce that locally with the
same command:

```bash
cd src/main/app
SPECS=$(git diff --name-only --diff-filter=d origin/main...HEAD -- '*.spec.ts' | sed 's|^src/main/app/||')
ESLINT_USE_FLAT_CONFIG=false npx eslint -c .eslintrc.strict-specs.js $SPECS
```

New specs should use `getByRole` with an accessible name, falling back to `getByLabelText` and
`getByText` only when no role fits. Where a third-party widget renders nothing queryable —
`ngx-editor` sets no role on its ProseMirror element, OpenLayers draws to a canvas, a file input
is `display: none` — disable the rule on that line with a comment saying which widget forces it.

Migrating the remaining specs so these rules can become errors everywhere is the next phase.

## Kotlin

`./gradlew detekt` runs `detektMain`, `detektTest` and `detektItest`. Each one analyses a source set
with its compile classpath. Rules that need that type information, such as
`UnsafeCallOnNullableType` and `InjectDispatcher`, report nothing without it. `./gradlew build`
runs the same tasks, so CI checks the same rules. `detektApply` runs without type information and
only applies the fixes that detekt can make by itself.

## i18n message keys

`src/main/app/src/app/core/translations.spec.ts` reads `nl.json` and `en.json` and runs with
`npm test`. It checks two things:

- Every key is lowercase kebab-case in every segment. A segment contains only `a-z` and `0-9`,
  with a single `-` between words, and segments are separated by `.`. So the key is
  `afleidingswijze-brondatum.ingangsdatum-besluit`, not `afleidingswijzeBrondatum.INGANGSDATUM_BESLUIT`.
- `nl.json` and `en.json` hold the same keys.

The keys `" 1 "` to `" 14 "` and `" 7b "` are exempt. They only divide the JSON files into
sections, and no code looks them up.

When the check fails, rename the key in both files and change every place that uses it. Many keys
are built at runtime from a prefix and a backend enum value, a field name or a value from an
external register, such as `"taak.status." + taak.status`. The code converts these with `toI18nKey`
or the `i18nKey` pipe, which writes every segment in lowercase kebab-case
(`taak.status.NIET_TOEGEKEND` becomes `taak.status.niet-toegekend`). Rename such a key to what
`toI18nKey` makes of it. If you add a key that is built at runtime, convert it the same way,
otherwise the user sees the raw key.

## Conventions enforced by linters

Coding conventions that a linter can check are enforced there rather than only described in
`CLAUDE.md` and `.claude/rules/`:

| Convention                                                    | Enforced by                                                      |
| ------------------------------------------------------------- | ---------------------------------------------------------------- |
| No Jira ticket references (`PZ-…`, `DRT-…`) in code comments  | detekt `ForbiddenComment`, ESLint `no-warning-comments`          |
| Single-expression Kotlin functions use an expression body     | detekt `ExpressionBodySyntax`                                    |
| Kotlin files start with an SPDX header                        | detekt `AbsentOrWrongFileLicense`                                |
| Acronyms in Kotlin class names: `IOStream`, `XmlFormatter`    | detekt `ClassNaming` (`classPattern`)                            |
| No `catch (exception: Exception)` in main code                | detekt `TooGenericExceptionCaught` (default)                     |
| No `requireNotNull` or `runCatching`                          | detekt `ForbiddenMethodCall`                                     |
| Boolean properties start with is/has/are/can/should           | detekt `BooleanPropertyNaming`                                   |
| No unused MockK stubs in unit tests                           | `UnnecessaryStubCheckingTestListener` in `ZacTestProjectConfig`  |
| No `NO_ERRORS_SCHEMA`, no `any`                               | ESLint `no-restricted-imports`, `@typescript-eslint/no-explicit-any` |
| Component inputs use `input()`, not the `@Input()` decorator  | ESLint `@angular-eslint/prefer-signals`                          |
| Every segment of an i18n key is lowercase kebab-case          | `src/main/app/src/app/core/translations.spec.ts` (`npm test`)    |
| `nl.json` and `en.json` hold the same i18n keys               | `src/main/app/src/app/core/translations.spec.ts` (`npm test`)    |

### Fixing exception findings

`TooGenericExceptionCaught` and `ForbiddenMethodCall` report a catch that is broader than the
code in the `try` block needs. A generic catch swallows bugs: a `NullPointerException` from a
mistake in the `try` block gets handled like an expected failure. Catch the specific exception
types that the block can throw, and let everything else propagate:

```kotlin
try {
    drcClient.enkelvoudigInformatieobjectDelete(uuid)
} catch (drcRuntimeException: DrcRuntimeException) {
    LOG.warning { "Failed to delete document: ${drcRuntimeException.message}" }
} catch (processingException: ProcessingException) {
    LOG.warning { "Failed to delete document: ${processingException.message}" }
}
```

When the goal is cleanup on any failure rather than handling the failure, use `finally`. It
needs no catch at all:

```kotlin
var isWritten = false
try {
    return writeTo(path).also { isWritten = true }
} finally {
    if (!isWritten) Files.deleteIfExists(path)
}
```

`@Suppress("TooGenericExceptionCaught")` is a sign that the catch is too broad, not a way to
silence the finding. Keep it only for a deliberate boundary that must survive any failure,
such as a notification handler.

Instead of `requireNotNull`, make the value non-nullable where it is declared, or handle the
null case: `?: throw` a specific exception, or `checkNotNull` with a message when null can only
mean a programming error.

### Boolean property names

detekt `BooleanPropertyNaming` checks the name of every Kotlin boolean property and local variable against
`^(is|has|are|can|should)`. The rule's default pattern is `^(is|has|are)`; two prefixes are added because the
code has two kinds of boolean that none of the default prefixes describes:

- `can` for a permission: the `RestXxxRechten` models and the OPA policy outputs hold some forty permissions
  each named after a Dutch verb, such as `canLezen` and `canToevoegenInitiatorPersoon`. `isLezen` or
  `hasLezen` would say something else.
- `should` for an instruction in a request, such as `shouldSendMail` on `RESTTaakStuurGegevens` and
  `shouldTakenVerlengen` on `RestZaakVerlengGegevens`: the client asks ZAC to do something, it does not
  describe a state.

The JSON name of a boolean on ZAC's own REST API follows the Kotlin property name, so renaming such a
property changes the API. JSON-B strips an `is` prefix, so an `is` property carries
`@get:JsonbProperty`, and `@set:JsonbProperty` when it is also read from a request. Where the JSON name
is a contract ZAC does not own, the annotation keeps the original name instead: the input of the OPA
policies, saved searches (`RestZoekParameters` and `FilterParameters` are stored as JSON in
`Zoekopdracht`) and external clients such as SmartDocuments. JPA entities keep their columns through
`@Column`.

## Using Visual Studio Code

Visual Studio Code does not report errors in HTML templates by default. The official
'Angular Language Service' extension adds them.
