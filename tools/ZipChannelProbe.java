import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.*;
import java.util.HashMap;

/**
 * Same as ZipfsProbe but opens entries through Files.newByteChannel - the exact call
 * securejarhandler's UnionFileSystem.byteChannel makes, which is where the game fails.
 */
public class ZipChannelProbe {
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toAbsolutePath();
        int bad = 0, total = 0;
        try (FileSystem fs = FileSystems.newFileSystem(URI.create("jar:" + jar.toUri()), new HashMap<String, Object>())) {
            Path root = fs.getPath("/");
            try (var stream = Files.walk(root)) {
                for (Path p : (Iterable<Path>) stream::iterator) {
                    if (Files.isDirectory(p)) continue;
                    total++;
                    try (SeekableByteChannel ch = Files.newByteChannel(p, StandardOpenOption.READ)) {
                        ByteBuffer buf = ByteBuffer.allocate(8192);
                        while (ch.read(buf) != -1) buf.clear();
                    } catch (Exception ex) {
                        bad++;
                        if (bad <= 30) System.out.println("BAD  " + p + "  :: " + ex);
                    }
                }
            }
        }
        System.out.println("total=" + total + " bad=" + bad);
    }
}
