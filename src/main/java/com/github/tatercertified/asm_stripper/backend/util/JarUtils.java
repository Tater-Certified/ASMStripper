package com.github.tatercertified.asm_stripper.backend.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public final class JarUtils {
    private static Path jarPath;

    /**
     * Sets the jar file path from a Class in your project
     * @param clazz Class from your project
     */
    public static void setJarPathFromClass(Class<?> clazz) {
        try {
            jarPath = Paths.get(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets all {@link ClassNode}s from your project
     * @return List of all ClassNodes in your project
     * @throws IOException Failed to read the jar
     */
    public static List<ClassNode> getClassNodes() throws IOException {
        List<ClassNode> nodes = new ArrayList<>();
        if (jarPath != null) {
            try (JarFile jarFile = new JarFile(jarPath.toString())) {
                Enumeration<JarEntry> entries = jarFile.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName().endsWith(".class")) {
                        nodes.add(MixinService.getService().getBytecodeProvider().getClassNode(entry.getName(), false));
                    }
                }
            } catch (IOException ignored) {
                try (Stream<Path> stream = Files.walk(jarPath)) {
                    stream.filter(path -> path.toString().endsWith(".class"))
                            .forEach(path -> {
                                try (InputStream is = new FileInputStream(path.toFile())) {
                                    ClassReader cr = new ClassReader(is);
                                    nodes.add(MixinService.getService().getBytecodeProvider().getClassNode(cr.getClassName()));
                                } catch (IOException | ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return nodes;
    }
}
