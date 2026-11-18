# Update the table in Confluence

For the table tasks PodiumD componenten, Supporting componenten and Externe API's. After the task has shown its table,
update that same table on the sprint hero page. Change only the table of the current task.

`hero_page_tables.py` (next to this file) reads and writes the page. Run it with
`python3 .claude/skills/sprint-hero-page/hero_page_tables.py` and the sandbox disabled (network, keychain).

## 1. Find and read the page (at the start of the task)

- Use the link the user gave. Without a link, `hero_page_tables.py latest` finds the newest "Sprint <n> - Hero" page
  in the space PZW. Name that page to the user and continue only after they confirm it is the right one. Without an
  API token there is no search: ask for the link.
- `hero_page_tables.py show <link>` prints the version tables as JSON, with the row and column names exactly as they
  are on the page. Compare the new values with the table of the current task. Never copy a version value from it.
- For PodiumD componenten, the `PodiumD X.Y` columns in the page's PodiumD table are the PodiumD releases to show.
- When the page cannot be read (`cannot read the page: no API token and acli is not logged in`), continue without
  Confluence: ask the user to paste the current table (optional, only to report what changed) and, for PodiumD
  componenten, which PodiumD releases to show. Give the table as usual and skip steps 2 and 3.

## 2. Plan (after the table is shown)

Write a values file in the scratchpad with only the table of the current task and only its version columns, in the
shape of `show`:

```json
{"Supporting componenten": {"Wildfly": {"Latest": "…", "Our Version on TEST": "…"}}}
```

- Use the row and column names of `show`, not those of the task page; they can differ ("Wildfly" vs WildFly,
  "Platform Autorisatie Beheer Component (PABC)" vs PABC). Match them by meaning.
- Every row of the table belongs in the file. When a value could not be verified, leave that column out of the row
  (the cell keeps its value).
- Never put a value in Last Checked, Location or Remarks / Stories; the script refuses that. It sets Last Checked to
  today only for a row whose version columns are all in the file. Otherwise it keeps the old date and warns, so a
  value that could not be verified never looks freshly checked.

`hero_page_tables.py plan <link> <values.json> <plan.json>` prints every cell that changes, old -> new. It writes
nothing to Confluence. Show the user that change list and its warnings.

- In Supporting componenten and Externe API's the script sets the colour of Our Version on TEST according to the
  legend above the table (major / minor / patch behind, aligned). When our version is higher than Latest, or is not a
  version (`-`), it leaves the colour and warns. It also leaves the colour and warns when Latest or Our Version on
  TEST changed but the other one is missing from the values file (not verified in this run).
- The PodiumD table has a different legend; the script does not set its colours. Name the PodiumD rows whose values
  changed, so the user can let Rovo set their colour (the help panel above the table has the prompt).

## 3. Write

Ask the user whether to write this change list to the page. Only after an explicit yes, run
`hero_page_tables.py apply <plan.json>`. Each yes covers one plan only.

- It writes only when the page is still the version the plan was made from. If someone edited the page in between,
  it refuses: run `show` and `plan` again and ask again.
- It prints the new page version and link. Confluence keeps the previous version in the page history.
- Remind the user to update the outdated remarks by hand; the script never changes them.

Without an API token (or when the page could not be read at all), skip steps 2 and 3: the user pastes the table as
before, and asks Rovo for the colours and Last Checked with the prompt in the help panel above the table.
