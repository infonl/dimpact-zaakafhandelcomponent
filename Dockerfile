# syntax=docker/dockerfile:1.28.0@sha256:bb22d9815c728170f72750f4e5b0d672e06176142e1d602c7e66c050100b7e5b

#
# SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

FROM eclipse-temurin:25.0.4.1_1-jre-ubi10-minimal@sha256:e961af01f4a1a3ec3ca71a3063739a33da943cf71734a8a747856d9385846544 AS runtime
ARG branchName
ARG commitHash
ARG versionNumber
ENV BRANCH_NAME=$branchName COMMIT_HASH=$commitHash VERSION_NUMBER=$versionNumber

LABEL name="zaakafhandelcomponent"
LABEL summary="Zaakafhandelcomponent (ZAC) developed for Dimpact"
LABEL description="The zaakafhandelcomponent (ZAC) is an open-source, generic, workflow-based component for managing 'zaken' in the context of zaakgericht werken, a Dutch approach to case management."
LABEL maintainer="INFO.nl"
LABEL vendor="INFO.nl"
LABEL url="https://github.com/infonl/dimpact-zaakafhandelcomponent"
LABEL git_commit=$commitHash
# Unset labels set by the Temurin Ubi9 base Docker image
LABEL build-date=""
LABEL com.redhat.component=""
LABEL com.redhat.license_terms=""
LABEL io.buildah.version=""
LABEL io.k8s.description=""
LABEL io.k8s.display-name=""
LABEL io.openshift.tags=""
LABEL release=""
LABEL vcs-ref=""
LABEL version=""

# Add user to run our application
RUN useradd -u 1001 -g users --no-log-init -s /sbin/nologin -c "Default Application User" default

# Copy build timestamp (used by HealthCheckService.java)
RUN date -Iseconds > /build_timestamp.txt

USER default

# Copy zaakafhandelcomponent bootable jar
COPY target/zaakafhandelcomponent.jar /

# Turn on ability to be able to override WildFly settings using environment variables
ENV WILDFLY_OVERRIDING_ENV_VARS=1

# Start zaakafhandelcomponent
# make sure that the WildFly management port is accessible from outside the container
ENTRYPOINT ["java", "-Djboss.bind.address.management=0.0.0.0", "-jar", "zaakafhandelcomponent.jar"]
EXPOSE 8080 9990
