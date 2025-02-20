## Overview
This directory contains scripts for various actions around developing and operating ZAC.

## Inventory

## docker
Build docker image

## docker-compose
Docker compose volumes and init scripts

Contains Linux override to workaround `host.docker.internal`

## file-templates
[Public code](https://yml.publiccode.tools/) template

## github
Test GitHub Workflow using [ACT](https://github.com/nektos/act)

## python
List latest versions of ZAC and PodiumD components

## solr
Reindex Solr data.

Useful when:
* OpenNotifications is not started
* Zaak does not show in dashboard lists

## wildfly
Install Wildfly locally, to enable IntelliJ run configuration with ZAC

## zap
Run [Zed Attack Proxy (ZAP)](https://www.zaproxy.org/) scan in Docker container against URL
