Feature: Resident submits form in open-forms

  Scenario: Resident fills in indienen-aansprakelijkheid-behandelen zaaktype
    Given A Resident fills in the indienen-aansprakelijkheid-behandelen open-forms form using user profile "alice"
    When "Alice" clicks on element with text: "Verzenden"
    Then "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    And "Bob" logs in
    And "Bob" clicks on the first zaak in zaak werkvoorraad with delay
    And "Bob" sees the created zaak with "Alice" coming from open-forms
