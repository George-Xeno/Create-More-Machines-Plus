import java.io.InputStream;
import java.lang.classfile.*;
import java.lang.classfile.instruction.*;

/**
 * Disassembles a method of a JDK class using the Class-File API available in Java 24+.
 * Usage: DumpMethod <fully.qualified.ClassName> <methodName>
 */
public class DumpMethod {
    public static void main(String[] args) throws Exception {
        String cn = args[0];
        String mn = args[1];
        Class<?> c = Class.forName(cn);
        String res = cn.substring(cn.lastIndexOf('.') + 1) + ".class";
        byte[] bytes;
        try (InputStream in = c.getResourceAsStream(res)) {
            bytes = in.readAllBytes();
        }
        ClassModel cm = ClassFile.of().parse(bytes);
        System.out.println("class " + cm.thisClass().asInternalName() + "  (major " + cm.majorVersion() + ", minor " + cm.minorVersion() + ")");
        for (MethodModel mm : cm.methods()) {
            if (!mm.methodName().stringValue().equals(mn)) continue;
            System.out.println("---- " + mm.methodName().stringValue() + mm.methodType().stringValue());
            var codeOpt = mm.code();
            if (codeOpt.isEmpty()) { System.out.println("   (no code)"); continue; }
            for (CodeElement ce : codeOpt.get()) {
                if (ce instanceof Instruction ins) {
                    System.out.printf("  %04d  %s%n", ins.offsetOf().orElse(-1), ins);
                } else {
                    System.out.println("        " + ce);
                }
            }
        }
    }
}
