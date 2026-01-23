/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

package nl.info.zac.smartdocuments.templates.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import net.atos.zac.admin.model.ZaakafhandelParameters
import net.atos.zac.util.FlywayIntegrator
import nl.info.zac.util.AllOpen
import java.time.ZonedDateTime

@Entity
@Table(schema = FlywayIntegrator.SCHEMA, name = "smartdocuments_document_creatie_sjabloon_groep")
@SequenceGenerator(
    schema = FlywayIntegrator.SCHEMA,
    name = "sq_sd_document_creatie_sjabloon_groep",
    sequenceName = "sq_sd_document_creatie_sjabloon_groep",
    allocationSize = 1
)
@AllOpen
class SmartDocumentsTemplateGroup {
    @Id
    @GeneratedValue(generator = "sq_sd_document_creatie_sjabloon_groep", strategy = GenerationType.SEQUENCE)
    @Column(name = "id_sjabloon_groep")
    var id: Long? = null

    @Column(name = "smartdocuments_id", nullable = false)
    lateinit var smartDocumentsId: String

    @Column(name = "naam", nullable = false)
    lateinit var name: String

    @Column(name = "aanmaakdatum", nullable = false)
    var creationDate: ZonedDateTime? = null

    @ManyToOne
    @JoinColumn(name = "parent_id")
    var parent: SmartDocumentsTemplateGroup? = null

    @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var children: MutableSet<SmartDocumentsTemplateGroup>? = null

    @OneToMany(mappedBy = "templateGroup", fetch = FetchType.EAGER, cascade = [CascadeType.ALL])
    var templates: MutableSet<SmartDocumentsTemplate>? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zaakafhandelparameters_id", nullable = false)
    lateinit var zaakafhandelParameters: ZaakafhandelParameters
}
