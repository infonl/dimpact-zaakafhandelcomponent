/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.repository.CmmnDeployment;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class CMMNDeployer {

    private static final String CMMN_MODELS_FOLDER_NAME = "cmmn";

    private static final String CMMN_MODELS_FILE_NAME = "models";

    private static final String CASE_ID_XPATH_EXPRESSION = "/definitions/case/@id";

    private static final String CASE_NAME_XPATH_EXPRESSION = "/definitions/case/@name";

    private static final Logger LOG = Logger.getLogger(CMMNDeployer.class.getName());

    @Inject
    private CmmnRepositoryService cmmnRepositoryService;

    public void onStartup(@Observes @Initialized(ApplicationScoped.class) Object event) {
        try (final InputStream modelsInputStream = getClass().getClassLoader()
                .getResourceAsStream(format("%s/%s", CMMN_MODELS_FOLDER_NAME, CMMN_MODELS_FILE_NAME));
             final BufferedReader modelsReader = new BufferedReader(new InputStreamReader(modelsInputStream, StandardCharsets.UTF_8))) {
            modelsReader.lines().filter(StringUtils::isNotBlank).forEach(this::checkModel);
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkModel(final String modelFileName) {
        try {
            final byte[] modelBytes;
            try (final InputStream modelInputStream = getClass().getClassLoader()
                    .getResourceAsStream(format("%s/%s", CMMN_MODELS_FOLDER_NAME, modelFileName))) {
                modelBytes = modelInputStream.readAllBytes();
            }

            final Document modelXml;
            try (final InputStream modelInputStream = new ByteArrayInputStream(modelBytes)) {
                final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                // disallow doctype declaration, external entity declarations and prohibit the
                // use of all protocols by external entities to prevent XXE attacks
                // see: https://rules.sonarsource.com/java/RSPEC-2755/
                documentBuilderFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                documentBuilderFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                documentBuilderFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
                modelXml = documentBuilderFactory.newDocumentBuilder().parse(modelInputStream);
            }

            final XPath xPath = XPathFactory.newInstance().newXPath();
            final String key = (String) xPath.evaluate(CASE_ID_XPATH_EXPRESSION, modelXml, XPathConstants.STRING);
            final CmmnDeployment cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().deploymentKey(key).latest().singleResult();

            if (cmmnDeployment == null) {
                deployModel(modelFileName, modelBytes, modelXml, key, xPath);
            } else {
                try (final InputStream modelInputStream = new ByteArrayInputStream(modelBytes);
                     final InputStream deployedModelInputStream = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.getId(),
                             modelFileName)) {
                    if (!IOUtils.contentEquals(modelInputStream, deployedModelInputStream)) {
                        deployModel(modelFileName, modelBytes, modelXml, key, xPath);
                    }
                }
            }
        } catch (final IOException | XPathExpressionException | ParserConfigurationException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private void deployModel(final String modelFileName, final byte[] modelBytes, final Document modelXml, final String key,
            final XPath xPath) throws XPathExpressionException {
        final String name = (String) xPath.evaluate(CASE_NAME_XPATH_EXPRESSION, modelXml, XPathConstants.STRING);
        cmmnRepositoryService.createDeployment().key(key).name(name).addBytes(modelFileName, modelBytes).deploy();
        LOG.info(format("Successfully deployed CMMN model with key '%s' and name '%s' from file '%s'", key, name, modelFileName));
    }
}
