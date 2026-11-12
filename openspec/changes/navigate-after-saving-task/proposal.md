## Why

[PZ-7875](https://dimpact.atlassian.net/browse/PZ-7875) takes the user back to where a taak was opened from after
completing it. Saving a taak (*Tussentijds opslaan*, or *Opslaan* in a Form.io form) still leaves the user on the
taak, so they have to navigate back themselves. [PZ-13375](https://dimpact.atlassian.net/browse/PZ-13375) asks
for saving to behave like completing.

## What Changes

- After saving a taak, ZAC navigates exactly as it does after completing one:

  | Opened from | After saving or completing |
  | --- | --- |
  | Zaak | Zaak |
  | Dashboard | Dashboard |
  | Takenwerkvoorraad | Takenwerkvoorraad |
  | E-mail (`?bron=email`) | Stays on the taak |
  | Direct link | Dashboard |
  | Search | Dashboard |

- A taak opened from a direct link or from search goes to the dashboard again, after completing too. Since the
  breadcrumbs ([#7411](https://github.com/infonl/dimpact-zaakafhandelcomponent/pull/7411)) its trail starts with
  the parent route *Taken*, which redirects to the takenwerkvoorraad. ZAC now only returns to a page the user
  visited.
- The werklijst the user returns to reads its data again, so it shows the saved or completed taak instead of a
  cached copy from before ([PZ-13506](https://dimpact.atlassian.net/browse/PZ-13506)). This applies after
  completing a taak too.
- The e-mail case follows the implementation of PZ-7875 (stay on the taak), not the "terug naar het dashboard"
  line in the story, because the story's main criterion is to do the same as completing.

## Capabilities

### New Capabilities

- `taak-terugnavigatie`: where ZAC goes after a taak is saved or completed.

### Modified Capabilities

None.

## Impact

- **Frontend**: `TakenService.updateTaakdata` navigates on success, like `TakenService.complete`. This covers the
  fixed Angular taakformulieren and the Form.io formulieren, which both save through it. Both first mark the
  werklijst queries stale.
- **Docs**: gebruikershandleiding, *Inzien en behandelen van taken*.
- **Backend**: `TaskRestService.updateTaskData` reindexes the taak in Solr, as assigning a taak already does.
  Saving did not update the index, so a werklijst kept showing the old toelichting and fatale datum.
- **REST API, OPA**: no change.
