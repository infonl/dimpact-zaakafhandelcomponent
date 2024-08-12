Feature: Zaak

  Scenario: Bob wants to create a new zaak
    Given "Bob" is logged in to zac
    When "Bob" wants to create a new zaak
    Then "Bob" sees the created zaak
    Then "Bob" sees the zaak initiator
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad"
    Then "Bob" sees the created zaak
