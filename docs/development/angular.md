# Angular

### Angular dependabot 

Depandabot creates pull-requests for all angular dependencies that are behind, but angular dependencies have to be updated in a specific order and angular provides migration tools with `ng update`.

So for angular dependencies we have to update them manually and commit them to the repository, then dependabot will remove it's pull-requests.

### Update angular dependencies manually

Run the following command to update angular dependencies:
```bash
    ng update 
```
This command will check whatever angular packages are behind and could result in a list of packages to update like:

![ng update](./attachments/ng-update.png)

Then you can run one of the commands in the list like:

```bash
    ng update @angular/core
```

Make sure to commit every update command you run with a format like:

```bash
    git add .
    git commit -m "update @angular/core to [version]"
```

