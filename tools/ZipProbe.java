import java.io.InputStream;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Reads every entry of a jar and reports the ones the JDK cannot decode. */
public class ZipProbe {
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]);
        int bad = 0, total = 0;
        try (ZipFile zf = new ZipFile(jar.toFile())) {
            var en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                total++;
                try (InputStream in = zf.getInputStream(e)) {
                    in.readAllBytes();
                } catch (Exception ex) {
                    bad++;
                    System.out.println("BAD  " + e.getName() + "  :: " + ex);
                }
            }
        }
        System.out.println("total=" + total + " bad=" + bad);
    }
}
