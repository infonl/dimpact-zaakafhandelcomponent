### Create runtime image fase
FROM docker.io/eclipse-temurin:21-ubi9-minimal as runtime

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

# Start zaakafhandelcomponent
ENTRYPOINT ["java", "-Xms1024m", "-Xmx1024m", "-jar", "zaakafhandelcomponent.jar"]
EXPOSE 8080 9990

ARG branchName
ARG commitHash
ARG versionNumber
ENV BRANCH_NAME=$branchName COMMIT_HASH=$commitHash VERSION_NUMBER=$versionNumber
