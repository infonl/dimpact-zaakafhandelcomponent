# Python scripts

This folder contains some python scripts that might be useful. Further details on the exact scripts
is described below.

## Setup Python virtual environment
There's a simple shell [init script](./init-pyenv.sh) that can be used to create the virtual environment. It 
will use command `python3 -m venv .venv` and therefore requires a python 3 version to be available, and will 
then install this in folder `.venv`, with the required [requirements](./requirements.txt) applied. Once the 
python environment is available, the scripts from the sub-folders can be run.

## Dependencies scripts
The [dependencies folder](./dependencies) contains classes and a main script 
[versions-of-components.py](./dependencies/versions-of-components.py) that will look up the latest version of
a defined list of dependencies of components that ZAC integrates with.

To run the script use the following command (from this location):
```shell
python ./dependencies/versions-of-components.py
```

## PodiumD scripts
The [podiumd folder](./podiumd) contains classes and a main script 
[podiumd_versions.py](./podiumd/podiumd_versions.py) that can be used to compare the 'latest' podiumd 
component versions to a specific older podiumd version. It will pull this information from the Chart 
and Version files from [GitHub PodiumD Helm Charts](https://github.com/Dimpact-Samenwerking/helm-charts).

To run the script use the following command:
```shell
python ./podiumd/podiumd_versions.py
```
