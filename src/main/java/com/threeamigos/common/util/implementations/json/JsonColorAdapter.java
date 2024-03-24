package com.threeamigos.common.util.implementations.json;

import com.google.gson.*;
import com.threeamigos.common.util.interfaces.json.JsonAdapter;

import java.awt.*;
import java.lang.reflect.Type;

/**
 * A class able to (de)serialize a {@link java.awt.Color} in Json format
 *
 * @author Stefano Reksten
 */
public class JsonColorAdapter implements JsonAdapter<Color> {

    @Override
    public JsonElement serialize(final Color src, final Type typeOfSrc, final JsonSerializationContext context) {
        return new JsonPrimitive(
                String.format("%02X%02X%02X%02X", src.getAlpha(), src.getRed(), src.getGreen(), src.getBlue()));
    }

    @Override
    public Color deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException {
        String argb = json.getAsJsonPrimitive().getAsString();
        if (argb.length() != 8) {
            throw invalidColor(argb);
        }
        int red;
        int green;
        int blue;
        int alpha;
        try {
            alpha = Integer.parseUnsignedInt(argb.substring(0, 2), 16);
            red = Integer.parseUnsignedInt(argb.substring(2, 4), 16);
            green = Integer.parseUnsignedInt(argb.substring(4, 6), 16);
            blue = Integer.parseUnsignedInt(argb.substring(6), 16);
        } catch (NumberFormatException e) {
            throw invalidColor(argb);
        }
        return new Color(red, green, blue, alpha);
    }

    private JsonParseException invalidColor(final String colorRepresentation) {
        return new JsonParseException(String.format("Invalid color representation: %s", colorRepresentation));
    }
}
