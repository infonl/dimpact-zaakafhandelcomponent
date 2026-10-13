# Open Zaak SQL templates

SQL templates that [`create-zaaktypes.py`](../create-zaaktypes.py) renders and executes against the local Open Zaak
PostgreSQL database to create test zaaktypes. See the [test data README](../README.md#create-zaaktypespy) for how to
run that script.

| Template | Purpose |
|---|---|
| `zaaktype-template.sql` | Creates one zaaktype with its resultaattypen, statustypen, roltypen and zaaktype-informatieobjecttype. |
| `zaaktype-version-update-template.sql` | Marks a created zaaktype as a superseded version. Used with `--add-extra-versions`. |

## Placeholders

The script replaces these placeholders before it executes a template:

| Placeholder | Replaced with |
|---|---|
| `[[ZAAKTYPE_NUMBER]]` | The 1-based sequence number of the zaaktype |
| `[[VERSION_NUMBER]]` | The number of the superseded version (`zaaktype-version-update-template.sql` only) |
| `[[ZAAKTYPE_UUID]]` | The UUID of the zaaktype |
| The other `[[..._UUID]]` placeholders | A new UUID per zaaktype, for example `[[STATUSTYPE_1_UUID]]` |

When you add a related object to `zaaktype-template.sql`, give it a new `[[..._UUID]]` placeholder and add that
placeholder to `UUID_PLACEHOLDERS` in `create-zaaktypes.py`. Otherwise the script does not replace it.
