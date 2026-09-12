import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Low level ZIP structure dump: compares the entry offsets stored in the central
 * directory against the local file headers actually present in the file.
 *
 * A healthy jar has PK\x03\x04 at every recorded offset.  "invalid LOC header (bad
 * signature)" from jdk.zipfs means at least one of them does not.
 */
public class ZipStruct {
    static final long LOC_SIG = 0x04034b50L;
    static final long CEN_SIG = 0x02014b50L;
    static final long EOCD_SIG = 0x06054b50L;

    public static void main(String[] args) throws Exception {
        try (RandomAccessFile raf = new RandomAccessFile(args[0], "r")) {
            long len = raf.length();
            System.out.println("file length = " + len);

            // --- locate EOCD by scanning backwards
            long eocd = -1;
            int maxBack = (int) Math.min(len, 66000);
            byte[] tail = new byte[maxBack];
            raf.seek(len - maxBack);
            raf.readFully(tail);
            for (int i = maxBack - 22; i >= 0; i--) {
                if ((tail[i] & 0xff) == 0x50 && (tail[i + 1] & 0xff) == 0x4b
                        && (tail[i + 2] & 0xff) == 0x05 && (tail[i + 3] & 0xff) == 0x06) {
                    eocd = len - maxBack + i;
                    break;
                }
            }
            if (eocd < 0) { System.out.println("no EOCD found"); return; }

            ByteBuffer e = read(raf, eocd, 22);
            int total = u16(e, 10);
            long cenSize = u32(e, 12);
            long cenOff = u32(e, 16);
            System.out.printf("EOCD@%d entries=%d cenSize=%d cenOff=%d%n", eocd, total, cenSize, cenOff);
            System.out.printf("cenOff+cenSize=%d  (EOCD expected at %d)  delta=%d%n",
                    cenOff + cenSize, eocd, eocd - (cenOff + cenSize));

            // --- walk the central directory
            int badLoc = 0, badCen = 0, checked = 0;
            long p = cenOff;
            for (int i = 0; i < total; i++) {
                ByteBuffer c = read(raf, p, 46);
                if (u32(c, 0) != CEN_SIG) {
                    System.out.printf("  #%d BAD CEN signature 0x%08x at %d%n", i, u32(c, 0), p);
                    if (++badCen > 5) break;
                    continue;
                }
                int flags = u16(c, 8);
                int method = u16(c, 10);
                long crc = u32(c, 16);
                long csize = u32(c, 20);
                long usize = u32(c, 24);
                int nlen = u16(c, 28);
                int elen = u16(c, 30);
                int clen = u16(c, 32);
                long locOff = u32(c, 42);
                byte[] nameB = new byte[nlen];
                raf.seek(p + 46);
                raf.readFully(nameB);
                String name = new String(nameB, StandardCharsets.UTF_8);

                // local header
                ByteBuffer l = read(raf, locOff, 30);
                long sig = u32(l, 0);
                if (sig != LOC_SIG) {
                    badLoc++;
                    if (badLoc <= 30) {
                        System.out.printf("  BAD LOC %-50s locOff=%-8d sig=0x%08x flags=0x%04x method=%d%n",
                                name, locOff, sig, flags, method);
                    }
                } else {
                    int lnlen = u16(l, 26);
                    int lelen = u16(l, 28);
                    long lcsize = u32(l, 18);
                    long lusize = u32(l, 22);
                    if (lnlen != nlen || lelen != elen || lcsize != csize || lusize != usize) {
                        System.out.printf("  MISMATCH %-44s cen(nlen=%d elen=%d csize=%d usize=%d) loc(nlen=%d elen=%d csize=%d usize=%d)%n",
                                name, nlen, elen, csize, usize, lnlen, lelen, lcsize, lusize);
                    }
                }
                checked++;
                p += 46 + nlen + elen + clen;
            }
            System.out.printf("checked=%d badCen=%d badLoc=%d%n", checked, badCen, badLoc);
        }
    }

    static ByteBuffer read(RandomAccessFile raf, long off, int n) throws Exception {
        byte[] b = new byte[n];
        raf.seek(off);
        raf.readFully(b);
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
    }

    static int u16(ByteBuffer b, int i) { return b.getShort(i) & 0xffff; }
    static long u32(ByteBuffer b, int i) { return b.getInt(i) & 0xffffffffL; }
}
