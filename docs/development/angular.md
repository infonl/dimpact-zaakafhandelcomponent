# Angular

The ZAC frontend is based on the [Angular frontend framework](https://angular.io/).

## Angular updates and Dependabot

Depandabot creates pull requests for all Angular dependencies that are behind, but Angular dependencies have to be updated in a specific order and angular provides migration tools with `ng update`.

So for Angular dependencies we have to update them manually and commit them to the repository, then Dependabot will remove its pull requests.

## Update angular dependencies manually

### Prerequisites

1. You need to have the Angular command line tools installed locally. Do install these on a Mac you can use
`brew install angular-cli`

### Update Angular dependencies

Run the following command to update Angular dependencies from the [ZAC frontend folder](../../src/main/app):

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

