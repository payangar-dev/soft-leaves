package com.payangar.softleaves;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The mod's only setting, read once at startup from {@code config/soft_leaves.json}
 * and never written back, so a hand-edited file keeps its comments and layout. Both
 * loaders hand their own config directory to {@link SoftLeavesInit}; the game ships
 * Gson on both, so one value needs no config library.
 */
public final class SoftLeavesConfig {

    private static final double MIN_RESISTANCE = 0.0;
    private static final double MAX_RESISTANCE = 3.0;
    private static final double DEFAULT_RESISTANCE = 1.0;

    private static final String FILE_NAME = Constants.MOD_ID + ".json";
    private static final String RESISTANCE_KEY = "resistance";
    // Gson parses leniently, which is what lets the shipped file carry comments.
    private static final String DEFAULT_FILE = """
        // Soft Leaves configuration.
        //
        // resistance: how hard foliage brakes whatever moves through it, as a multiplier
        // of the tuned default. 0 keeps leaves passable but stops them slowing anything
        // down, 1 is the default, 3 is the maximum. Speed still scales the braking, this
        // only shifts how strong the whole effect is.
        //
        // Read once at startup. In multiplayer, give the server and its clients the same
        // value: a mismatch makes their movement predictions disagree.
        {
          "resistance": 1.0
        }
        """;

    private static volatile double resistance = DEFAULT_RESISTANCE;

    private SoftLeavesConfig() {
    }

    public static double resistance() {
        return resistance;
    }

    public static void load(Path configDir) {
        Path file = configDir.resolve(FILE_NAME);
        try {
            if (Files.exists(file)) {
                resistance = readResistance(file);
            } else {
                Files.createDirectories(configDir);
                Files.writeString(file, DEFAULT_FILE);
            }
        } catch (IOException | RuntimeException e) {
            // A file that fails to parse is left untouched rather than overwritten:
            // whatever the user typed stays there for them to fix.
            Constants.LOG.warn("Soft Leaves: could not read {}, keeping the default resistance.", file, e);
        }
        Constants.LOG.info("Soft Leaves: leaf resistance {}.", resistance);
    }

    private static double readResistance(Path file) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonElement value = root.get(RESISTANCE_KEY);
            if (value == null) {
                return DEFAULT_RESISTANCE;
            }
            double parsed = value.getAsDouble();
            // Lenient parsing accepts the NaN literal, which the clamp would carry
            // through and multiply into every movement it touches.
            double clamped = Double.isFinite(parsed)
                ? Math.clamp(parsed, MIN_RESISTANCE, MAX_RESISTANCE)
                : DEFAULT_RESISTANCE;
            if (clamped != parsed) {
                Constants.LOG.warn("Soft Leaves: resistance {} is outside {}-{}, using {}.",
                    parsed, MIN_RESISTANCE, MAX_RESISTANCE, clamped);
            }
            return clamped;
        }
    }
}
