# CMMN model

ZAC uses [this](../../src/main/resources/cmmn/Generiek_zaakafhandelmodel.cmmn.xml) CMMN model.

## Edit the model

To edit the CMMN model go to Flowable's [Cloud Design](https://trial.flowable.com/design/) and:
1. Create account
2. Login
3. Create application (named _ZAC_)
4. Create model and then select `Import`
5. Upload ZAC's [CMMN model](../../src/main/resources/cmmn/Generiek_zaakafhandelmodel.cmmn.xml) for the import

## Change the model

When ready with the changes in [Cloud Design](https://trial.flowable.com/design/):
1. Download the model
2. Store the downloaded model to replace the [currently used one](../../src/main/resources/cmmn/Generiek_zaakafhandelmodel.cmmn.xml)
3. Check if all tests (unit, integration tests and e2e) pass
4. Commit/Push changes
