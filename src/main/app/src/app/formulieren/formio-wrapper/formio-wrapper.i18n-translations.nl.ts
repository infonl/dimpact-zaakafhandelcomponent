/*
 * SPDX-FileCopyrightText: 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */

/** Dutch (nl) translations applied via formio-wrapper.component.ts onFormioReady using Webform.addLanguage('nl', …, true), which merges with English defaults so missing keys fall back to English rather than showing raw key names. Form labels are already Dutch in the form definition; only validation errors, buttons, date labels, and aria labels are translated here. The top-of-form alert panel is suppressed via disableAlerts: true in the formio component options. */
export const FORMIO_NL_TRANSLATIONS = {
  // Validation errors
  required: "{{field}} is verplicht.",
  unique: "{{field}} moet uniek zijn.",
  array: "{{field}} moet een lijst zijn.",
  array_nonempty: "{{field}} moet een niet-lege lijst zijn.",
  nonarray: "{{field}} mag geen lijst zijn.",
  select: "{{field}} bevat een ongeldige selectie.",
  pattern: "{{field}} komt niet overeen met het patroon {{pattern}}.",
  minLength: "{{field}} moet minimaal {{length}} tekens bevatten.",
  maxLength: "{{field}} mag maximaal {{length}} tekens bevatten.",
  minWords: "{{field}} moet minimaal {{length}} woorden bevatten.",
  maxWords: "{{field}} mag maximaal {{length}} woorden bevatten.",
  min: "{{field}} mag niet kleiner zijn dan {{min}}.",
  max: "{{field}} mag niet groter zijn dan {{max}}.",
  maxDate: "{{field}} mag geen datum na {{maxDate}} bevatten.",
  minDate: "{{field}} mag geen datum voor {{minDate}} bevatten.",
  maxYear: "{{field}} mag geen jaar groter dan {{maxYear}} bevatten.",
  minYear: "{{field}} mag geen jaar kleiner dan {{minYear}} bevatten.",
  minSelectedCount: "U moet minimaal {{minCount}} items selecteren.",
  maxSelectedCount: "U mag maximaal {{maxCount}} items selecteren.",
  invalid_email: "{{field}} moet een geldig e-mailadres zijn.",
  invalid_url: "{{field}} moet een geldige URL zijn.",
  invalid_regex: "{{field}} komt niet overeen met het patroon {{regex}}.",
  invalid_date: "{{field}} is geen geldige datum.",
  invalid_day: "{{field}} is geen geldige dag.",
  invalidDay: "{{field}} is geen geldige dag.",
  invalidOption: "{{field}} is een ongeldige waarde.",
  invalidValueProperty: "Ongeldige waarde-eigenschap.",
  mask: "{{field}} komt niet overeen met het masker.",
  valueIsNotAvailable: "{{field}} is een ongeldige waarde.",
  // Row errors (DataGrid / EditGrid)
  unsavedRowsError: "Sla alle rijen op voordat u verdergaat.",
  invalidRowsError: "Corrigeer ongeldige rijen voordat u verdergaat.",
  invalidRowError: "Ongeldige rij. Corrigeer of verwijder de rij.",
  // Required day/month/year sub-fields
  requiredDayField: "{{field}} is verplicht.",
  requiredDayEmpty: "{{field}} is verplicht.",
  requiredMonthField: "{{field}} is verplicht.",
  requiredYearField: "{{field}} is verplicht.",
  // Form-level messages
  complete: "Inzending voltooid.",
  error: "Corrigeer de volgende fouten voordat u indient.",
  errorListHotkey: "Druk op Ctrl + Alt + X om terug te gaan naar de foutlijst.",
  errorsListNavigationMessage:
    "Klik om naar het veld met de volgende fout te navigeren.",
  submitError:
    "Controleer het formulier en corrigeer alle fouten voordat u indient.",
  // Navigation buttons
  next: "Volgende",
  previous: "Vorige",
  cancel: "Annuleren",
  submit: "Indienen",
  confirmCancel: "Weet u zeker dat u wilt annuleren?",
  // Aria labels
  cancelButtonAriaLabel: "Annuleerknop. Klik om het formulier te wissen.",
  previousButtonAriaLabel:
    "Vorige knop. Klik om naar het vorige tabblad te gaan.",
  nextButtonAriaLabel:
    "Volgende knop. Klik om naar het volgende tabblad te gaan.",
  submitButtonAriaLabel: "Indienknop. Klik om het formulier in te dienen.",
  // Date/time field labels
  month: "Maand",
  day: "Dag",
  year: "Jaar",
  time: "Ongeldige tijd",
  january: "Januari",
  february: "Februari",
  march: "Maart",
  april: "April",
  may: "Mei",
  june: "Juni",
  july: "Juli",
  august: "Augustus",
  september: "September",
  october: "Oktober",
  november: "November",
  december: "December",
  // Character/word count — {{type}} is itself a translated value: this.t('words') or this.t('characters')
  words: "woorden",
  characters: "tekens",
  typeRemaining: "{{remaining}} {{type}} resterend.",
  typeCount: "{{count}} {{type}}",
};
