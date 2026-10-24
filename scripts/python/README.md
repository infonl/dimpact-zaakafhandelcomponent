# Python scripts

This folder contains some python scripts that might be useful. Further details on the exact scripts
is described below.

## Setup Python virtual environment
There's a simple shell [init script](init-pyenv.sh) that can be used to create the virtual environment. It will use command 
`python3 -m venv .venv` and therefore requires a python 3 version to be available, and will then install this in folder 
`.venv`, with the required [requirements](requirements.txt) applied. Once the python environment is available, the scripts from the 
sub-folders can be run.

The init script will have to be run from the context of the current session (in `scripts/python`) to be able to 
immediately make use of the installed environment and installed libraries. 
Use the following command:
```shell
   source init-pyenv.sh
```

## Dependencies scripts
The [dependencies folder](dependencies) contains classes and a main script [versions.py](dependencies/versions.py) that 
will look up the latest version of a defined list of dependencies of components that ZAC integrates with.

To run the script use the following command (in `scripts/python`):
```shell
   dependencies/versions.py
```

Sample output:
```shell
KvK Zoeken API latest version: 2.0 - https://developers.kvk.nl/documentation/release-notes/zoeken-api
KvK Basisprofiel API latest version: 1.4.4 - https://developers.kvk.nl/documentation/release-notes/basisprofiel-api
KvK Vestigingsprofiel API latest version: 1.4.1 - https://developers.kvk.nl/documentation/release-notes/vestigingsprofiel-api
Haal-Centraal-BRP latest version: 2.5.0 - https://brp-api.github.io/Haal-Centraal-BRP-bevragen/releasenotes
Open Notificaties latest version: 1.8.0 - https://github.com/open-zaak/open-notificaties/releases/tag/1.8.0
Open Zaak latest version: 1.18.0 - https://github.com/open-zaak/open-zaak/releases/tag/1.18.0
Open Klant latest version: 2.5.0 - https://github.com/maykinmedia/open-klant/releases/tag/2.5.0
Open Formulieren latest version: 3.1.0-alpha.1 - https://github.com/open-formulieren/open-forms/releases/tag/3.1.0-alpha.1
Objecten API latest version: 3.0.0 - https://github.com/maykinmedia/objects-api/releases/tag/3.0.0
Object Types API latest version: 3.0.0 - https://github.com/maykinmedia/objecttypes-api/releases/tag/3.0.0
Open Inwoner latest version: 1.27.0 - https://github.com/maykinmedia/open-inwoner/releases/tag/v1.27.0
KISS latest version: 0.6.0 - https://github.com/Klantinteractie-Servicesysteem/KISS-frontend/releases/tag/v0.6.0
Zaakafhandelcomponent latest version: 3.2.28 - https://github.com/infonl/dimpact-zaakafhandelcomponent/releases/tag/v3.2.28
Keycloak latest version: 26.1.2 - https://github.com/keycloak/keycloak/releases/tag/26.1.2
ClamAV latest version: clamav-1.4.2 - https://github.com/Cisco-Talos/clamav/releases/tag/clamav-1.4.2
Postgresql latest version: 17.4 - https://www.postgresql.org/docs/release/17.4/
Infinispan latest version: 15.1.5.Final-1 - https://hub.docker.com/layers/infinispan/server/15.1.5.Final-1/images/sha256-a298b0e90e2e473eca9a087d0f3b8e1326fb0a896e9f9aed84d79d6a8db45208
```

## PodiumD scripts
The [podiumd folder](podiumd) contains classes and a main script 
[versions.py](podiumd/versions.py) that can be used to compare the 'latest' podiumd 
component versions to a specific older podiumd version. It will pull this information from the Chart 
and Version files from [GitHub PodiumD Helm Charts](https://github.com/Dimpact-Samenwerking/helm-charts).

To run the script use the following command:
```shell
   podiumd/versions.py -o 3.2.0
```

### PodiumD versions script options
The script has a number of parameters to be able to compare specific versions, this from the help:

```
usage: versions.py [-h] -o OLD_VERSION [-n NEW_VERSION] [-b NEW_VERSION_BRANCH]

Script for comparing PodiumD version details between specific versions

options:
  -h, --help            show this help message and exit
  -o, --old_version OLD_VERSION
                        Version to base comparison on
  -n, --new_version NEW_VERSION
                        Version to comparison with. Defaults to the latest on main.
  -b, --new_version_branch NEW_VERSION_BRANCH
                        Branch to comparison on. Defaults to the main branch.
```

### PodiumD versions script output explained

First here's a sample of the output it generates
(shortened with `...` where lines are too similar to previous lines) :
```shell
# PodiumD Latest version
PodiumD: 3.3.0
Chart: 3.3.0
Chart Dependencies:
Keycloak: Keycloak: 21.8.0
OpenLDAP: OpenLDAP: 1.0.2
...
Open Inwoner: Open Inwoner: 1.5.3
KISS Elastic: KISS Elastic: 1.0.0
Components:
Open Zaak: Open Zaak: 1.17.0
Open Notificaties: Open Notificaties: 1.8.0
...
httpRequestJob.jwtCli: httpRequestJob.jwtCli: 6.2.0
httpRequestJob.alpine: httpRequestJob.alpine: 3.20
# PodiumD 3.2.0 version
PodiumD: 3.2.0
Chart: 3.2.0
Chart Dependencies:
Keycloak: Keycloak: 21.8.0
OpenLDAP: OpenLDAP: 1.0.2
...
Open Inwoner: Open Inwoner: 1.5.3
KISS Elastic: KISS Elastic: 1.0.0
Components:
Open Zaak: Open Zaak: 1.15.0
Open Notificaties: Open Notificaties: 1.7.1
...
httpRequestJob.jwtCli: httpRequestJob.jwtCli: 6.2.0
httpRequestJob.alpine: httpRequestJob.alpine: 3.20
# Compare PodiumD versions

# PodiumD version updates:
| Component | PodiumD 3.2.0 | PodiumD 3.3.0 |
|---|---|---|
| ClamAV | 1.4.1 | **1.4.2** |
| Contact | v0.5.1-20250107153635-2d5264c | **main-20250129154544-b179b49** |
| Objecten | 2.4.4 | **3.0.0** |
...
| httpRequestJob.jwtCli | 6.2.0 | 6.2.0 |
| kiss.adapter | latest | latest |
| kiss.alpine | 3.20 | 3.20 |
| kiss.sync | latest | latest |
```

#### What do we see here?
The output has 3 sections:

##### `# PodiumD Latest version` and `# PodiumD <x.y.z> version`
This shows the information retrieved from the Chart and Values files for PodiumD. It prints the PodiumD app-version it 
has found - here `3.3.0` - the chart version - also `3.3.0` and then the dependencies.

The list of `Chart Dependencies` is retrieved directly from the dependencies section in the Chart file and contain the 
versions of the Helm Charts that are used by the PodiumD Helm Chart.

The list of `Components` contains the components found in the Values file with a `tag` set. These are the docker 
component tags that will be used during deployment.

##### `# PodiumD version updates`
This compares the 2 sets of PodiumD version information, as displayed above, and outputs a markdown formatted table of 
them together.

The table shows the component, the old `component` version and the new `component` version.

Where the _new_ version differs from the _old_ version, the version for the _new_ is surrounded with `**` so that it 
would display bold in a markdown viewer.
