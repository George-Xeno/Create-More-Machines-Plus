import cpw.mods.jarhandling.SecureJar;

import java.nio.file.*;

/**
 * Reads specific entries through SecureJar the way the game does: a union filesystem
 * layered over jdk.zipfs.  data/ entries fail in the pack, assets/ entries apparently
 * do not, so probe both.
 */
public class SecureJarProbe2 {
    static final String[] TARGETS = {
            "data/cmmplus/recipe/encased_fan_brass.json",
            "data/cmmplus/recipe/belt_connector_beyond.json",
            "data/cmmplus/loot_table/blocks/belt_brass.json",
            "data/cmmplus/loot_table/blocks/encased_fan_brass.json",
            "assets/cmmplus/lang/en_us.json",
            "assets/cmmplus/blockstates/belt_brass.json",
            "assets/cmmplus/models/item/crushing_wheel_brass.json",
            "META-INF/neoforge.mods.toml",
            "cmmplus.mixins.json",
    };

    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toAbsolutePath();
        SecureJar sj = SecureJar.from(jar);
        try {
            for (String t : TARGETS) {
                try {
                    Path p = sj.getPath(t);
                    byte[] b = Files.readAllBytes(p);
                    System.out.printf("OK    %-50s %d bytes  (%s)%n", t, b.length, p);
                } catch (Throwable ex) {
                    System.out.printf("BAD   %-50s :: %s%n", t, ex);
                }
            }
        } finally {
            sj.close();
        }
    }
}
