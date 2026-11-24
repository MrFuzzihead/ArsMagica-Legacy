package am2.mixins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.injection.Inject;

/**
 * Reads the class-level {@code @Mixin} annotation, which has {@code CLASS} retention and therefore cannot be read
 * with Java reflection. Shared by the bytecode guard tests.
 */
public final class MixinTestSupport {

    private MixinTestSupport() {}

    public static String mixinTarget(Class<?> mixinClass) throws Exception {
        String resource = mixinClass.getName()
            .replace('.', '/') + ".class";
        final Set<String> targets = new HashSet<String>();
        try (InputStream in = mixinClass.getClassLoader()
            .getResourceAsStream(resource)) {
            assertNotNull("compiled mixin class not found: " + resource, in);
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM5) {

                @Override
                public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                    if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(descriptor)) {
                        return null;
                    }
                    return new AnnotationVisitor(Opcodes.ASM5) {

                        @Override
                        public AnnotationVisitor visitArray(String name) {
                            if (!"value".equals(name)) {
                                return null;
                            }
                            return new AnnotationVisitor(Opcodes.ASM5) {

                                @Override
                                public void visit(String name, Object value) {
                                    if (value instanceof Type) {
                                        targets.add(((Type) value).getClassName());
                                    }
                                }
                            };
                        }
                    };
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        assertEquals("@Mixin must declare exactly one target class", 1, targets.size());
        return targets.iterator()
            .next();
    }

    /**
     * Reads an {@code @Inject} handler. The caller supplies the full handler signature, including the
     * {@code CallbackInfo}/{@code CallbackInfoReturnable} parameter.
     */
    public static Inject inject(Class<?> mixinClass, String handlerName, Class<?>... parameterTypes) throws Exception {
        java.lang.reflect.Method handler = mixinClass.getDeclaredMethod(handlerName, parameterTypes);
        Inject inject = handler.getAnnotation(Inject.class);
        assertNotNull("@Inject is missing on " + handlerName, inject);
        return inject;
    }
}
