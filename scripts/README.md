## Overview
This directory contains scripts for various actions around developing and operating ZAC.

## Included scripts 

```
┌─ docker
│  │     
│  └─ build-docker-image.sh       Build docker image
│
├─ docker-compose
│  │     
│  └─ setup-linux.sh              Adds `host.docker.internal` for all containers in Docker Compose setup we use
│
├─ file-templates                 Public code (https://yml.publiccode.tools/) template
│
├─ github
│  │     
│  └─ test-workflows.sh           Test GitHub Workflow using ACT (https://github.com/nektos/act)
│
├─ python
│  │     
│  ├─ init-pyenv.sh               Initialize Python virtual environment
│  │
│  ├─ dependencies
│  │  │
│  │  └─ versions.py              List latest versions of ZAC dependencies
│  │
│  └─ podiumd
│     │
│     └─ versions.py              List latest versions of PodiumD components
│
├─ solr
│  │     
│  └─ reindex-zac-solr-data.sh    Reindex Solr data. Useful when OpenNotifications is not started 
│
├─ wildfly
│  │     
│  ├─ install-wildfly.cli         Install Wildfly on Windows, to enable IntelliJ run configuration with ZAC
│  │
│  └─ install-wildfly.sh          Install Wildfly on Linux/Mac, to enable IntelliJ run configuration with ZAC 
│
└─ zap
   │
   └─ zap-docker-full-scan.sh     Run ZAP scan in Docker container versus URL  
```
