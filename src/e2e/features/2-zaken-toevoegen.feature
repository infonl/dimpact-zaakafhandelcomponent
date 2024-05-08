Feature: Zaak

  Scenario: Bob wants to create a new zaak
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" wants to create a new zaak
    Then "Bob" sees the created zaak with a delay
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    Then "Bob" sees the created zaak
