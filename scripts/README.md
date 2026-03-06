# Overview
This directory contains scripts for various actions around developing and operating ZAC:

## [bruno](bruno)

Contains collections for the Bruno API client tool as well as scripts to update these.

## [docker](docker)
Build ZAC Docker image script. Used by our Gradle build file.

## [docker-compose](docker-compose)
Docker compose volumes and init scripts

Contains Linux override to workaround `host.docker.internal`

## [file-templates](file-templates)
[Public code](https://yml.publiccode.tools/) template

## [github](github)
Test GitHub Workflow using [ACT](https://github.com/nektos/act)

## [productaanvraag](productaanvraag)
Script to notify ZAC about a new product request.

## [python](python)
List latest versions of ZAC and used PodiumD components

## [solr](solr)
Reindex Solr data.

Useful when:
* OpenNotifications is not started
* Zaak does not show in dashboard lists

## [wildfly](wildfly)
Install Wildfly locally, to enable IntelliJ run configuration with ZAC

## [zap](zap)
Run [Zed Attack Proxy (ZAP)](https://www.zaproxy.org/) scan in Docker container against URL
