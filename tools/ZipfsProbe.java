import java.net.URI;
import java.nio.file.*;
import java.util.HashMap;

/**
 * Walks a jar through jdk.zipfs (the implementation the game actually uses through
 * securejarhandler's UnionFileSystem) and reports entries it refuses to read.
 */
public class ZipfsProbe {
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toAbsolutePath();
        int bad = 0, total = 0;
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jar.toUri()), new HashMap<String, Object>())) {
            Path root = fs.getPath("/");
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (Files.isDirectory(p)) continue;
                    total++;
                    try {
                        Files.readAllBytes(p);
                    } catch (Exception ex) {
                        bad++;
                        System.out.println("BAD  " + p + "  :: " + ex);
                    }
                }
            }
        }
        System.out.println("total=" + total + " bad=" + bad);
    }
}
