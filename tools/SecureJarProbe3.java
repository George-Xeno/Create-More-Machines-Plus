import cpw.mods.jarhandling.SecureJar;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Exercises the SecureJar union filesystem the way the game's resource reload does:
 * first list everything under data/, then read the entries, then hammer them from
 * several threads.  A single-threaded read never fails, so concurrency is the thing
 * worth testing.
 */
public class SecureJarProbe3 {
    public static void main(String[] args) throws Exception {
        Path jar = Path.of(args[0]).toAbsolutePath();
        SecureJar sj = SecureJar.from(jar);
        try {
            Path root = sj.getRootPath();
            System.out.println("root            = '" + root + "'");
            System.out.println("root.abs        = '" + root.toAbsolutePath() + "'");
            System.out.println("fs              = " + root.getFileSystem());
            System.out.println("fs.provider     = " + root.getFileSystem().provider());
            System.out.println("exists(root)    = " + Files.exists(root));
            System.out.println("isDir(root)     = " + Files.isDirectory(root));

            // ---- A. list data/
            List<String> dataFiles = new ArrayList<>();
            try (var s = Files.walk(root)) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    if (Files.isDirectory(p)) continue;
                    String n = p.toString().replace('\\', '/');
                    if (!n.startsWith("/")) n = "/" + n;
                    if (n.contains("/data/")) dataFiles.add(n);
                }
            }
            System.out.println("A. walk found " + dataFiles.size() + " data entries");
            dataFiles.stream().limit(5).forEach(x -> System.out.println("     " + x));

            // ---- B. sequential read of every data entry
            int bad = 0;
            for (String n : dataFiles) {
                try {
                    Files.readAllBytes(root.getFileSystem().getPath(n));
                } catch (Throwable t) {
                    bad++;
                    if (bad <= 5) System.out.println("B. BAD " + n + " :: " + t);
                }
            }
            System.out.println("B. sequential read bad=" + bad);

            // ---- C. concurrent read, many rounds
            int threads = 16, rounds = 40;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            List<Future<Integer>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int seed = t;
                futures.add(pool.submit(() -> {
                    int errs = 0;
                    Random rnd = new Random(seed);
                    for (int r = 0; r < rounds; r++) {
                        for (String n : dataFiles) {
                            try {
                                Files.readAllBytes(root.getFileSystem().getPath(n));
                            } catch (Throwable ex) {
                                errs++;
                                if (errs <= 3) System.out.println("C. BAD " + n + " :: " + ex);
                            }
                        }
                        if (r % 7 == 0) {
                            // mix in a little churn like a resource reload does
                            try (var s = Files.walk(root)) { s.count(); } catch (Throwable ignored) {}
                        }
                    }
                    return errs;
                }));
            }
            int totalErrs = 0;
            for (Future<Integer> f : futures) totalErrs += f.get();
            pool.shutdown();
            System.out.println("C. concurrent errors=" + totalErrs + "  (" + threads + " threads x " + rounds + " rounds x " + dataFiles.size() + " files)");
        } finally {
            sj.close();
        }
    }
}
