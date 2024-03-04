Feature: Resident submits form in open-forms

  @live-env-only
  Scenario: Resident fills in indienen-aansprakelijkheid-behandelen open-forms form
    Given Resident "Alice" fills in the indienen-aansprakelijkheid-behandelen open-forms form
    When Resident "Alice" submits the open-forms form
    And Employee "Bob" opens zac
    And Employee "Bob" navigates to "/zaken/werkvoorraad"
    And Employee "Bob" clicks on the first zaak in the zaak-werkvoorraad with delay
    Then Employee "Bob" sees the zaak that "Alice" created in open-forms
