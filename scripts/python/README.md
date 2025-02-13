# Python scripts

This folder contains some python scripts that might be useful. Further details on the exact scripts
is described below.

## Setup Python virtual environment
There's a simple shell [init script](./init-pyenv.sh) that can be used to create the virtual environment. It will use command 
`python3 -m venv .venv` and therefore requires a python 3 version to be available, and will then install this in folder 
`.venv`, with the required [requirements](./requirements.txt) applied. Once the python environment is available, the scripts from the 
sub-folders can be run.

The init script will have to be run from the context of the current session (in `scripts/python`) to be able to 
immediately make use of the installed environment and installed libraries. 
Use the following command:
```shell
   source init-pyenv.sh
```

## Dependencies scripts
The [dependencies folder](./dependencies) contains classes and a main script 
[versions-of-components.py](./dependencies/versions-of-components.py) that will look up the latest version of
a defined list of dependencies of components that ZAC integrates with.

To run the script use the following command (in `scripts/python`):
```shell
   ./dependencies/versions.py
```

## PodiumD scripts
The [podiumd folder](./podiumd) contains classes and a main script 
[podiumd_versions.py](./podiumd/podiumd_versions.py) that can be used to compare the 'latest' podiumd 
component versions to a specific older podiumd version. It will pull this information from the Chart 
and Version files from [GitHub PodiumD Helm Charts](https://github.com/Dimpact-Samenwerking/helm-charts).

To run the script use the following command:
```shell
   ./podiumd/versions.py -o 3.2.0
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
