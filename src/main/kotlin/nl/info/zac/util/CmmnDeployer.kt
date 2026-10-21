/*
 * SPDX-FileCopyrightText: 2022 Atos, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.zac.util

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.context.Initialized
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import org.flowable.cmmn.api.CmmnRepositoryService
import org.w3c.dom.Document
import java.util.logging.Logger
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

class CmmnDeployer @Inject constructor(
    private val cmmnRepositoryService: CmmnRepositoryService
) {
    companion object {
        private const val CMMN_MODELS_FOLDER_NAME = "cmmn"
        private const val CMMN_MODELS_FILE_NAME = "models"
        private const val CASE_ID_XPATH_EXPRESSION = "/definitions/case/@id"
        private const val CASE_NAME_XPATH_EXPRESSION = "/definitions/case/@name"
        private val LOG = Logger.getLogger(CmmnDeployer::class.java.name)
    }

    fun onStartup(@Observes @Initialized(ApplicationScoped::class) @Suppress("UNUSED_PARAMETER") event: Any) =
        javaClass.classLoader.readNonBlankResourceLines("$CMMN_MODELS_FOLDER_NAME/$CMMN_MODELS_FILE_NAME")
            .forEach(::checkModel)

    private fun checkModel(modelFileName: String) {
        val modelBytes = javaClass.classLoader.readResourceBytes("$CMMN_MODELS_FOLDER_NAME/$modelFileName")
        val modelXml = modelBytes.inputStream().use { modelInputStream ->
            DocumentBuilderFactory.newInstance().apply {
                // disallow doctype declaration, external entity declarations and prohibit the
                // use of all protocols by external entities to prevent XXE attacks
                // see: https://rules.sonarsource.com/java/RSPEC-2755/
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
                setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            }.newDocumentBuilder().parse(modelInputStream)
        }
        val xPath = XPathFactory.newInstance().newXPath()
        val key = xPath.evaluate(CASE_ID_XPATH_EXPRESSION, modelXml, XPathConstants.STRING) as String
        val cmmnDeployment = cmmnRepositoryService.createDeploymentQuery().deploymentKey(key).latest().singleResult()
        if (cmmnDeployment == null) {
            deployModel(modelFileName, modelBytes, modelXml, key, xPath)
        } else {
            val isModelDeployed = cmmnRepositoryService.getResourceAsStream(cmmnDeployment.id, modelFileName).use {
                it.readAllBytes().contentEquals(modelBytes)
            }
            if (!isModelDeployed) {
                deployModel(modelFileName, modelBytes, modelXml, key, xPath)
            }
        }
    }

    private fun deployModel(
        modelFileName: String,
        modelBytes: ByteArray,
        modelXml: Document,
        key: String,
        xPath: XPath
    ) {
        val name = xPath.evaluate(CASE_NAME_XPATH_EXPRESSION, modelXml, XPathConstants.STRING) as String
        cmmnRepositoryService.createDeployment().key(key).name(name).addBytes(modelFileName, modelBytes).deploy()
        LOG.info { "Successfully deployed CMMN model with key '$key' and name '$name' from file '$modelFileName'" }
    }
}
