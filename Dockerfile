#
# SPDX-FileCopyrightText: 2021 Atos, 2024 Lifely
# SPDX-License-Identifier: EUPL-1.2+
#

FROM docker.io/eclipse-temurin:21.0.5_11-jre-ubi9-minimal@sha256:62d477884d233d3dd4e897e3d5f11f456d4d18a8f66c8479ff47661bd607a6ea AS runtime
ARG branchName
ARG commitHash
ARG versionNumber

LABEL name="zaakafhandelcomponent"
LABEL summary="Zaakafhandelcomponent (ZAC) developed for Dimpact"
LABEL description="The zaakafhandelcomponent (ZAC) is an open-source, generic, workflow-based component for managing 'zaken' in the context of zaakgericht werken, a Dutch approach to case management."
LABEL maintainer="Lifely/INFO"
LABEL vendor="Lifely/INFO"
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

# Import certificates into Java truststore
ADD certificates /certificates
RUN keytool -importcert -cacerts -alias SmartDocuments -file /certificates/smartdocuments/smartdocuments_com.cer -storepass changeit -noprompt
RUN keytool -importcert -cacerts -alias QuoVadis_PKIoverheid_Private_Services_CA -file /certificates/kvk/QuoVadis_PKIoverheid_Private_Services_CA_-_G1.crt  -storepass changeit -noprompt
RUN keytool -importcert -cacerts -alias Staat_der_Nederlanden_Private_Root_CA -file /certificates/kvk/Staat_der_Nederlanden_Private_Root_CA_-_G1.crt -storepass changeit -noprompt
RUN keytool -importcert -cacerts -alias Staat_der_Nederlanden_Private_Services_CA -file /certificates/kvk/Staat_der_Nederlanden_Private_Services_CA_-_G1.crt -storepass changeit -noprompt

# Copy zaakafhandelcomponent bootable jar
COPY target/zaakafhandelcomponent.jar /

# Copy build timestamp (used by HealthCheckService.java)
RUN date -Iseconds > /build_timestamp.txt

# Turn on ability to be able to override WildFly settings using environment variables
ENV WILDFLY_OVERRIDING_ENV_VARS=1

# Start zaakafhandelcomponent
# make sure that the WildFly management port is accessible from outside the container
ENTRYPOINT ["java", "-Djboss.bind.address.management=0.0.0.0", "-jar", "zaakafhandelcomponent.jar"]
EXPOSE 8080 9990

ENV BRANCH_NAME=$branchName COMMIT_HASH=$commitHash VERSION_NUMBER=$versionNumber
