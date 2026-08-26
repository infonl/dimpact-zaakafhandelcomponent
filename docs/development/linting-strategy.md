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

`.eslintrc.js` prefers Testing Library queries over reaching into the DOM through Angular.
Around sixty older specs still query with `By.css` or `querySelector`, so those rules are
warnings project-wide and a whole-project `npm run lint` stays green on them.

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

## Using Visual Studio Code

Visual Studio Code does not report errors in HTML templates by default. The official
'Angular Language Service' extension adds them.
