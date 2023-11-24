Feature: Login

  Scenario: Bob wants to create a new zaak
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" wants to create a new zaak
    Then "Bob" sees the created zaak
    Given "Bob" navigates to "zac" with path "/taken/werkvoorraad"
    Then "Bob" sees the created zaak
