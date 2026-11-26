## Why

The admin screen hides the delete button of a system mail template, but the REST API does not check it
([PZ-13417](https://dimpact.atlassian.net/browse/PZ-13417)). A beheerder who calls
`DELETE /rest/beheer/mailtemplates/{id}` directly removes the template. A beheerder can also first turn it into a
normal template, with `PUT /rest/beheer/mailtemplates/{id}` and `defaultMailtemplate: false` or another `mail`, and
then delete it on the admin screen. Without its system template ZAC cannot send that mail: a signalering mail fails
with `MailTemplateNotConfiguredException`, and a task or zaak mail has no default template to propose. The review of
PZ-13184 found this.

The admin screen also disables the delete button of a mail template that a zaaktype configuration links. Through the
API the delete reaches the database, whose foreign key refuses it, and the beheerder gets a 500 response with a
database error.

## What Changes

- A system mail template is a mail template with `isDefaultMailtemplate = true`. The Flyway migrations create one for
  each `Mail` type; the admin screen already uses this flag to hide the delete button.
- `MailTemplateService.delete` refuses to delete a system mail template with a 400 response and a new error code.
- `MailTemplateService.delete` refuses to delete a mail template that a zaaktype configuration links, in any of its
  versions, with a 400 response and a new error code, instead of the 500 of the database.
- `MailTemplateService.updateMailtemplate` refuses to change the `mail` type or the system flag of a system mail
  template with a 400 response and a new error code, and changes nothing. Its name, subject and body stay editable.
- The API cannot make a mail template a system mail template: creating one with `defaultMailtemplate: true`, or
  setting the flag on a normal template, is refused with the same error code. The flag is owned by the migrations.
- Normal mail templates that no zaaktype configuration links can be created, changed and deleted as before.
- The frontend gets Dutch and English texts for the three new error codes. Its behaviour does not change.

Out of scope: restoring system mail templates that an environment has already lost, a database constraint that
allows one system mail template per `Mail` type, and removing the unused `storeMailtemplate`.

## Capabilities

### New Capabilities
- `systeemmailtemplates`: the system mail templates that ZAC needs to send its mails cannot be deleted or turned into
  normal templates, also not through the REST API, while their content stays editable; a mail template that a
  zaaktype configuration links cannot be deleted either.

### Modified Capabilities

## Impact

- Backend: `MailTemplateService` (`delete`, `updateMailtemplate`, `createMailtemplate`) and three new values in
  `ErrorCode`.
- Frontend: three new keys in `src/main/app/src/assets/i18n/nl.json` and `en.json`.
- Unit test `MailTemplateServiceTest`; integration test `MailtemplateBeheerRestServiceTest`.
- REST: same paths and payloads; `DELETE` and `PUT` on `beheer/mailtemplates/{id}` and `POST` on
  `beheer/mailtemplates` refuse more requests. The OpenAPI spec does not change, and the scripts in
  `scripts/test-data/` do not call these endpoints.
- No database migration, no ADR.
