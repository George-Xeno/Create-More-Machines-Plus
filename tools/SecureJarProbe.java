import cpw.mods.jarhandling.SecureJar;

import java.nio.file.*;
import java.util.*;

/**
 * Opens a jar exactly the way NeoForge does (SecureJar -> UnionFileSystem -> jdk.zipfs)
 * and reads every data/ entry, which is the path that fails in the real modpack.
 */
public class SecureJarProbe {
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toAbsolutePath();
        int bad = 0, total = 0;
        List<String> badNames = new ArrayList<>();
        SecureJar sj = SecureJar.from(jar);
        try {
            Path root = sj.getRootPath();
            System.out.println("root = " + root);
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (Files.isDirectory(p)) continue;
                    String s = p.toString().replace('\\', '/');
                    if (!s.contains("/data/")) continue;
                    total++;
                    try {
                        byte[] b = Files.readAllBytes(p);
                        if (b.length == 0) System.out.println("EMPTY " + s);
                    } catch (Throwable ex) {
                        bad++;
                        badNames.add(s);
                        if (bad <= 25) System.out.println("BAD  " + s + "  :: " + ex);
                    }
                }
            }
        } finally {
            sj.close();
        }
        System.out.println("data entries=" + total + " bad=" + bad);
    }
}
