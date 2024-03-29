/*
 * SPDX-FileCopyrightText: 2022 Atos
 * SPDX-License-Identifier: EUPL-1.2+
 */

package net.atos.zac.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.config.PropertyVisibilityStrategy;

public final class JsonbUtil {
    private JsonbUtil() {
    }

    public static final PropertyVisibilityStrategy visibilityStrategy = new PropertyVisibilityStrategy() {
        @Override
        public boolean isVisible(Field field) {
            return true;
        }

        @Override
        public boolean isVisible(Method method) {
            return false;
        }
    };

    public static final Jsonb JSONB = JsonbBuilder.create();

    public static final Jsonb FIELD_VISIBILITY_STRATEGY = JsonbBuilder.create(
            new JsonbConfig().withPropertyVisibilityStrategy(JsonbUtil.visibilityStrategy)
    );
}
