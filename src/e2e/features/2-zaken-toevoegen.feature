Feature: Login

  Scenario: Bob wants to create a new zaak
    Given "Bob" navigates to "zac" with path "/"
    When "Bob" wants to create a new zaak
    Then "Bob" sees the created zaak with a delay
    Given "Bob" navigates to "zac" with path "/zaken/werkvoorraad" with delay after of 5000 ms
    Then "Bob" sees the created zaak

  Scenario: Employee "Bob" does not have enough information to finish Intake and assigns a task to Employee "Oscar"
    Given Employee "Bob" is on the newly created zaak with status "Intake"
    When Employee "Bob" does not have enough information to finish Intake and assigns a task to Employee "Oscar"
    And Employee "Bob" logs out of zac
    And Employee "Oscar" logs in to zac
    Then Employee "Oscar" sees the task assigned to Employee "Bob" in my task list
    And Employee "Oscar" is on the newly created zaak with status "Intake"
    And Employee "Oscar" sees the task assigned by Employee "Bob" in the newly created zaak tasks list
