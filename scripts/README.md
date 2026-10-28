# Overview
This directory contains scripts for various actions around developing and operating ZAC:

## [bruno](bruno)

Contains collections for the Bruno API client tool as well as scripts to update these.

## [docker](docker)
Build ZAC Docker image script. Used by our Gradle build file.

## [kind](kind)
Run the local ZAC development stack in a kind cluster, with its images, settings and seed data; started through
`start-kind.sh`. See [Running ZAC locally on Kubernetes (kind)](../docs/development/installKind.md).

## [file-templates](file-templates)
[Public code](https://yml.publiccode.tools/) template

## [github](github)
Test GitHub Workflow using [ACT](https://github.com/nektos/act)

## [open-zaak](open-zaak)
Delete a zaak with a given zaaknummer from Open Zaak.

## [productaanvraag](productaanvraag)
Script to notify ZAC about a new product request.

## [python](python)
List latest versions of ZAC and used PodiumD components

## [solr](solr)
Reindex Solr data.

Useful when:
* OpenNotifications is not started
* Zaak does not show in dashboard lists

## [test-data](test-data)
Scripts to create test zaaktypes, zaakafhandelparameters, zaken and documents in a local ZAC stack, for manual
and load/performance testing.

## [wildfly](wildfly)
Install WildFly locally, to enable IntelliJ run configuration with ZAC

## [zap](zap)
Run [Zed Attack Proxy (ZAP)](https://www.zaproxy.org/) scan in Docker container against URL
