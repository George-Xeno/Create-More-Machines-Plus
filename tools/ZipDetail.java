import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Prints every structural detail of the zip entries so two jars can be diffed. */
public class ZipDetail {
    public static void main(String[] args) throws Exception {
        String jar = args[0];
        String filter = args.length > 1 ? args[1] : "";
        try (RandomAccessFile raf = new RandomAccessFile(jar, "r")) {
            long len = raf.length();
            long eocd = findEocd(raf, len);
            ByteBuffer e = read(raf, eocd, 22);
            int total = u16(e, 10);
            long cenOff = u32(e, 16);
            long p = cenOff;
            System.out.println("# " + jar);
            for (int i = 0; i < total; i++) {
                ByteBuffer c = read(raf, p, 46);
                int flags = u16(c, 8);
                int method = u16(c, 10);
                int vNeed = u16(c, 4);
                long csize = u32(c, 20);
                long usize = u32(c, 24);
                int nlen = u16(c, 28);
                int elen = u16(c, 30);
                int clen = u16(c, 32);
                int disk = u16(c, 34);
                int iattr = u16(c, 36);
                long eattr = u32(c, 38);
                long locOff = u32(c, 42);
                byte[] nameB = new byte[nlen];
                raf.seek(p + 46); raf.readFully(nameB);
                String name = new String(nameB, StandardCharsets.UTF_8);
                byte[] cenExtra = new byte[elen];
                raf.seek(p + 46 + nlen); raf.readFully(cenExtra);
                byte[] comment = new byte[clen];
                raf.seek(p + 46 + nlen + elen); raf.readFully(comment);

                ByteBuffer l = read(raf, locOff, 30);
                int lvNeed = u16(l, 4);
                int lflags = u16(l, 6);
                int lmethod = u16(l, 8);
                long lcsize = u32(l, 18);
                long lusize = u32(l, 22);
                int lnlen = u16(l, 26);
                int lelen = u16(l, 28);
                byte[] locExtra = new byte[lelen];
                raf.seek(locOff + 30 + lnlen); raf.readFully(locExtra);

                boolean interesting = name.contains(filter);
                if (!interesting) { p += 46 + nlen + elen + clen; continue; }

                System.out.printf("%s%n", name);
                System.out.printf("   cen: vNeed=%d flags=0x%04x method=%d csize=%d usize=%d elen=%d clen=%d disk=%d iattr=0x%04x eattr=0x%08x locOff=%d%n",
                        vNeed, flags, method, csize, usize, elen, clen, disk, iattr, eattr, locOff);
                System.out.printf("   cen extra : %s%n", hex(cenExtra));
                System.out.printf("   loc: vNeed=%d flags=0x%04x method=%d csize=%d usize=%d nlen=%d elen=%d%n",
                        lvNeed, lflags, lmethod, lcsize, lusize, lnlen, lelen);
                System.out.printf("   loc extra : %s%n", hex(locExtra));
                p += 46 + nlen + elen + clen;
            }
        }
    }

    static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (byte x : b) sb.append(String.format("%02x", x));
        if (sb.length() == 0) return "(none)";
        // annotate known extra field IDs
        StringBuilder ids = new StringBuilder();
        int i = 0;
        while (i + 4 <= b.length) {
            int id = (b[i] & 0xff) | ((b[i + 1] & 0xff) << 8);
            int sz = (b[i + 2] & 0xff) | ((b[i + 3] & 0xff) << 8);
            ids.append(String.format(" [id=0x%04x size=%d]", id, sz));
            i += 4 + sz;
        }
        return sb + ids.toString();
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
