# syntax=docker/dockerfile:1@sha256:b6afd42430b15f2d2a4c5a02b919e98a525b785b1aaff16747d2f623364e39b6
# check=skip=SecretsUsedInArgOrEnv

#
# SPDX-FileCopyrightText: 2021 Atos, 2024 INFO.nl
# SPDX-License-Identifier: EUPL-1.2+
#

FROM eclipse-temurin:21.0.10_7-jre-ubi10-minimal@sha256:5de2c72777a5266cf498a41bf0cc57802e7208a15ba8338d38005506f8237eb2 AS runtime
ARG branchName
ARG commitHash
ARG versionNumber
ARG TRUSTSTORE_PASSWORD=changeit
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
