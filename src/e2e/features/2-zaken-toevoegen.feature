Feature: Zaak

  Scenario: Bob wants to create a new zaak
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" wants to create a new zaak
    Then "Bob" sees the created zaak with a delay
    When "Bob" clicks on Create Document for zaak
    Then "Bob" closes the SmartDocuments tab
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad" with delay after of 5000 ms
    Then "Bob" sees the created zaak
