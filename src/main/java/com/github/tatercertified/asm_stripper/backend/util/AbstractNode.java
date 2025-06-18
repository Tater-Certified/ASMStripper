package com.github.tatercertified.asm_stripper.backend.util;

import com.github.tatercertified.asm_stripper.ASMStripper;
import com.github.tatercertified.asm_stripper.api.annotation.Strip;
import com.github.tatercertified.asm_stripper.api.annotation.Strippable;
import com.github.tatercertified.asm_stripper.backend.stripper.ClassStripper;
import com.github.tatercertified.asm_stripper.backend.stripper.FieldStripper;
import com.github.tatercertified.asm_stripper.backend.stripper.MethodStripper;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AbstractNode {
    private final List<AnnotationNode> visibleAnnotations;
    private final List<AnnotationNode> invisibleAnnotations;
    private final String nodeName;
    @Nullable
    private final String nodePath;
    private final Object instance;
    @Nullable
    private final AbstractNode parent;

    private AbstractNode(Object instance, @Nullable String nodePath, String nodeName, @Nullable AbstractNode parent, List<AnnotationNode> visibleAnnotations, List<AnnotationNode> invisibleAnnotations) {
        this.instance = instance;
        this.nodePath = nodePath;
        this.nodeName = nodeName;
        this.parent = parent;
        this.visibleAnnotations = visibleAnnotations;
        this.invisibleAnnotations = invisibleAnnotations;
    }

    /**
     * Gets an AbstractNode from a {@link ClassNode}
     * @param node ClassNode instance
     * @return AbstractNode instance
     */
    public static AbstractNode from(ClassNode node) {
        return new AbstractNode(node, null, node.name, null, node.visibleAnnotations, node.invisibleAnnotations);
    }

    /**
     * Gets an AbstractNode from a {@link MethodNode}
     * @param node MethodNode instance
     * @return AbstractNode instance
     */
    public static AbstractNode from(MethodNode node, ClassNode parent) {
        return new AbstractNode(node, node.desc, node.name, AbstractNode.from(parent), node.visibleAnnotations, node.invisibleAnnotations);
    }

    /**
     * Gets an AbstractNode from a {@link FieldNode}
     * @param node FieldNode instance
     * @return AbstractNode instance
     */
    public static AbstractNode from(FieldNode node, ClassNode parent) {
        return new AbstractNode(node, node.desc, node.name, AbstractNode.from(parent), node.visibleAnnotations, node.invisibleAnnotations);
    }

    /**
     * Gets the annotations present on the AbstractNode
     * @param visible If they are marked as {@link java.lang.annotation.RetentionPolicy#RUNTIME}
     * @return List of AnnotationNodes
     */
    @Nullable
    public List<AnnotationNode> getAnnotations(boolean visible) {
        return visible ? this.visibleAnnotations : this.invisibleAnnotations;
    }

    /**
     * Gets an AnnotationNode for a specific Annotation type
     * @param annotation Annotation type as a Class
     * @return AnnotationNode for the type
     */
    @Nullable
    public AnnotationNode getAnnotation(Class<? extends Annotation> annotation) {
        Retention retention = annotation.getAnnotation(Retention.class);
        boolean visible;
        if (retention != null) {
            visible = retention.value() == RetentionPolicy.RUNTIME;
        } else {
            visible = false;
        }
        List<AnnotationNode> annotationNodes = this.getAnnotations(visible);
        if (annotationNodes != null) {
            for (AnnotationNode annotationNode : annotationNodes) {
                if (annotationNode.desc.equals(annotation.descriptorString())) {
                    return annotationNode;
                }
            }
        }
        return null;
    }

    /**
     * Gets the data inside an Annotation
     * @param annotationClass Annotation type
     * @return Map of the identifiers and values
     */
    @Nullable
    public Map<String, Object> getAnnotationData(Class<? extends Annotation> annotationClass) {
        AnnotationNode annotationNode = this.getAnnotation(annotationClass);
        if (annotationNode != null) {
            return this.getAnnotationData(annotationNode);
        } else {
            return null;
        }
    }

    /**
     * Gets the data inside an Annotation
     * @param annotationNode AnnotationNode instance
     * @return Map of the identifiers and values
     */
    @Nullable
    public Map<String, Object> getAnnotationData(AnnotationNode annotationNode) {
        Map<String, Object> data = new HashMap<>();
        if (annotationNode.values != null) {
            for (int i = 0; i < annotationNode.values.size(); i+=2) {
                data.put((String) annotationNode.values.get(i), annotationNode.values.get(i + 1));
            }
            return data;
        }
        return null;
    }

    /**
     * Gets the name of the AbstractNode
     * @return Name of the AbstractNode
     */
    public String getName() {
        return this.nodeName;
    }

    /**
     * Gets the descriptor of the AbstractNode
     * @return Descriptor of the AbstractNode
     */
    public String getDescription() {
        return this.nodePath;
    }

    /**
     * Gets the original Node instance
     * @return Original Node instance as an Object
     */
    public Object getInstance() {
        return this.instance;
    }

    /**
     * Gets the type of Node that the instance is
     * @return Class type of the AbstractNode
     */
    public Class<?> getType() {
        return this.instance instanceof ClassNode ? ClassNode.class : this.instance instanceof MethodNode ? MethodNode.class : FieldNode.class;
    }

    /**
     * Determines if the AbstractNode is in a Mixin
     * @return True if it is in a Mixin class or is a Mixin class
     */
    public boolean isMixin() {
        if (this.instance instanceof ClassNode) {
            return this.getAnnotation(Mixin.class) != null;
        } else {
            return this.parent.getAnnotation(Mixin.class) != null;
        }
    }

    @Nullable
    public List<String> getMixinTargets() {
        AnnotationNode node;
        if (this.instance instanceof ClassNode) {
            node = this.getAnnotation(Mixin.class);
        } else {
            node = this.parent.getAnnotation(Mixin.class);
        }
        if (node != null) {
            Map<String, Object> data = this.getAnnotationData(node);
            @SuppressWarnings("unchecked")
            List<Type> targets = (List<Type>) data.get("value");
            List<String> targetClassNames = new ArrayList<>();
            for (Type type : targets) {
                targetClassNames.add(type.getClassName());
            }
            return targetClassNames;
        } else {
            return null;
        }
    }

    /**
     * Determines if the AbstractNode is {@link Shadow}ing another Object
     * @return True if the Shadow annotation is present
     */
    public boolean isShadowed() {
        if (!(this.instance instanceof ClassNode)) {
            return this.getAnnotation(Shadow.class) != null;
        }
        return false;
    }

    /**
     * Gets if the AbstractNode can be stripped
     * @return True if the Strippable Annotation is found on the class or parent class
     */
    public boolean isStrippable() {
        if (this.instance instanceof ClassNode) {
            return this.getAnnotation(Strippable.class) != null;
        } else {
            return this.parent.getAnnotation(Strippable.class) != null;
        }
    }

    /**
     * Gets if the AbstractNode can run the {@link AbstractNode#strip()} method
     * @return True if it has the {@link Strip} Annotation
     */
    public boolean shouldStrip() {
        return this.getAnnotation(Strip.class) != null;
    }

    /**
     * Strips the current AbstractNode from its parent.<p>
     * If the AbstractNode is Shadowed, then the Shadowed Object is stripped.<p>
     * If the AbstractNode is a Mixin, then the target class is stripped.<p>
     * If the Strip Annotation has a class path override in its arguments, but will <i><b>not</b></i> take priority over Mixin classes
     */
    public void strip() {
        Map<String, Object> stripData = this.getAnnotationData(Strip.class);
        ClassNode parentOverride = this.parent == null ? (ClassNode) this.instance : (ClassNode) this.parent.instance;
        if (stripData != null) {

            String overrideStr = (String) stripData.get("altClassPath");
            if (overrideStr != null) {
                try {
                    parentOverride = MixinService.getService().getBytecodeProvider().getClassNode(overrideStr, false);
                } catch (ClassNotFoundException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        switch (this.instance) {
            case ClassNode node -> {
                if (ASMStripper.VERBOSE) {
                    System.out.println("Stripping Class " + node.name);
                }
                if (this.isMixin()) {
                    if (ASMStripper.VERBOSE) {
                        System.out.println(node.name + " is a Mixin");
                    }
                    List<String> targets = this.getMixinTargets();
                    for (String target : targets) {
                        try {
                            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(target, false);
                            ClassStripper.stripClass(classNode, List.of(node, classNode));
                        } catch (ClassNotFoundException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    ClassStripper.stripClass(parentOverride, List.of(parentOverride));
                }
            }
            case MethodNode node -> {
                if (ASMStripper.VERBOSE) {
                    System.out.println("Stripping Method " + node.name);
                }
                if (this.isShadowed()) {
                    if (ASMStripper.VERBOSE) {
                        System.out.println(node.name + " is Shadowed");
                    }
                    List<String> targets = this.getMixinTargets();
                    for (String target : targets) {
                        try {
                            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(target, false);
                            MethodStripper.stripMethod(node, classNode);
                            MethodStripper.stripMethod(node, (ClassNode) parent.getInstance());
                        } catch (ClassNotFoundException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    MethodStripper.stripMethod(node, parentOverride);
                }
            }
            case FieldNode node -> {
                if (ASMStripper.VERBOSE) {
                    System.out.println("Stripping Field " + node.name);
                }
                if (this.isShadowed()) {
                    if (ASMStripper.VERBOSE) {
                        System.out.println(node.name + " is Shadowed");
                    }
                    List<String> targets = this.getMixinTargets();
                    for (String target : targets) {
                        try {
                            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(target, false);
                            FieldStripper.stripField(node, classNode);
                            FieldStripper.stripField(node, (ClassNode) parent.getInstance());
                        } catch (ClassNotFoundException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    FieldStripper.stripField(node, parentOverride);
                }
            }
            case null, default -> throw new IllegalStateException("AbstractNode Type Unknown");
        }
    }
}
