/*
 * SPDX-FileCopyrightText: 2024 Lifely
 * SPDX-License-Identifier: EUPL-1.2+
 */

/**
 * klantinteracties
 * Description WIP.
 *
 * The version of the OpenAPI document: 0.0.3
 * Contact: standaarden.ondersteuning@vng.nl
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

package net.atos.client.klant.model;

import java.lang.reflect.Type;

import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;


/**
 * Gets or Sets SoortPartijEnum
 */
@JsonbTypeSerializer(SoortPartijEnum.Serializer.class)
@JsonbTypeDeserializer(SoortPartijEnum.Deserializer.class)
public enum SoortPartijEnum {

    PERSOON("persoon"),

    ORGANISATIE("organisatie"),

    CONTACTPERSOON("contactpersoon");

    private String value;

    SoortPartijEnum(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public static final class Deserializer implements JsonbDeserializer<SoortPartijEnum> {
        @Override
        public SoortPartijEnum deserialize(JsonParser parser, DeserializationContext ctx, Type rtType) {
            return fromValue(parser.getString());
        }
    }

    public static final class Serializer implements JsonbSerializer<SoortPartijEnum> {
        @Override
        public void serialize(SoortPartijEnum obj, JsonGenerator generator, SerializationContext ctx) {
            generator.write(obj.value);
        }
    }

    public static SoortPartijEnum fromValue(String text) {
        for (SoortPartijEnum b : SoortPartijEnum.values()) {
            if (String.valueOf(b.value).equals(text)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + text + "'");
    }
}
