/*
 * SPDX-FileCopyrightText: 2023, 2026 INFO.nl
 * SPDX-License-Identifier: EUPL-1.2+
 */
package nl.info.client.zgw.zrc.model.zaakobjecten

import nl.info.zac.util.NoArgConstructor

/**
 * ObjectPand
 */
@NoArgConstructor
data class ObjectPand(override var identificatie: String? = null) : ObjectBagObject(identificatie)
