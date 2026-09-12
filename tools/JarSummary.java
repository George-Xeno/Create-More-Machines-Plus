import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Structural summary of a jar: where the directories sit, whether the central
 * directory order matches the physical order, and where the data/ entries live.
 */
public class JarSummary {
    record Ent(String name, long locOff, int method, long csize, long usize, int flags) {}

    public static void main(String[] args) throws Exception {
        for (String jar : args) {
            try (RandomAccessFile raf = new RandomAccessFile(jar, "r")) {
                long len = raf.length();
                long eocd = findEocd(raf, len);
                ByteBuffer e = read(raf, eocd, 22);
                int total = u16(e, 10);
                long cenSize = u32(e, 12);
                long cenOff = u32(e, 16);
                int comment = u16(e, 20);

                List<Ent> ents = new ArrayList<>();
                long p = cenOff;
                for (int i = 0; i < total; i++) {
                    ByteBuffer c = read(raf, p, 46);
                    int nlen = u16(c, 28), elen = u16(c, 30), clen = u16(c, 32);
                    byte[] nb = new byte[nlen];
                    raf.seek(p + 46); raf.readFully(nb);
                    ents.add(new Ent(new String(nb, StandardCharsets.UTF_8),
                            u32(c, 42), u16(c, 10), u32(c, 20), u32(c, 24), u16(c, 8)));
                    p += 46 + nlen + elen + clen;
                }

                // physical order check
                boolean physicalAscending = true;
                long prev = -1;
                for (Ent x : ents) { if (x.locOff() < prev) { physicalAscending = false; break; } prev = x.locOff(); }

                long dirs = ents.stream().filter(x -> x.name().endsWith("/")).count();
                List<Ent> data = ents.stream().filter(x -> x.name().startsWith("data/")).toList();
                List<Ent> assets = ents.stream().filter(x -> x.name().startsWith("assets/")).toList();
                long firstData = data.isEmpty() ? -1 : data.get(0).locOff();
                long lastData = data.isEmpty() ? -1 : data.get(data.size() - 1).locOff();
                long lastAsset = assets.isEmpty() ? -1 : assets.get(assets.size() - 1).locOff();

                System.out.println("==== " + jar);
                System.out.printf("   len=%d entries=%d dirs=%d  eocd=%d cenOff=%d cenSize=%d comment=%d%n",
                        len, total, dirs, eocd, cenOff, cenSize, comment);
                System.out.printf("   cenOrderMatchesPhysical=%s   assetsLastLocOff=%d  dataLocOff=%d..%d%n",
                        physicalAscending, lastAsset, firstData, lastData);
                System.out.println("   directory entries:");
                ents.stream().filter(x -> x.name().endsWith("/")).limit(12)
                        .forEach(x -> System.out.printf("      %-45s locOff=%d%n", x.name(), x.locOff()));
                System.out.println("   last 6 entries by physical offset:");
                ents.stream().sorted(Comparator.comparingLong(Ent::locOff)).skip(Math.max(0, ents.size() - 6))
                        .forEach(x -> System.out.printf("      %-45s locOff=%d method=%d csize=%d usize=%d%n",
                                x.name(), x.locOff(), x.method(), x.csize(), x.usize()));
            }
        }
    }

    static long findEocd(RandomAccessFile raf, long len) throws Exception {
        int maxBack = (int) Math.min(len, 66000);
        byte[] tail = new byte[maxBack];
        raf.seek(len - maxBack); raf.readFully(tail);
        for (int i = maxBack - 22; i >= 0; i--) {
            if ((tail[i] & 0xff) == 0x50 && (tail[i + 1] & 0xff) == 0x4b
                    && (tail[i + 2] & 0xff) == 0x05 && (tail[i + 3] & 0xff) == 0x06) {
                return len - maxBack + i;
            }
        }
        throw new IllegalStateException("no EOCD");
    }

    static ByteBuffer read(RandomAccessFile raf, long off, int n) throws Exception {
        byte[] b = new byte[n];
        raf.seek(off); raf.readFully(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
    }

    static int u16(ByteBuffer b, int i) { return b.getShort(i) & 0xffff; }
    static long u32(ByteBuffer b, int i) { return b.getInt(i) & 0xffffffffL; }
}
