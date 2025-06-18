package com.github.tatercertified.asm_stripper.api;

import com.github.tatercertified.asm_stripper.ASMStripper;
import com.github.tatercertified.asm_stripper.backend.util.AbstractNode;
import com.github.tatercertified.asm_stripper.backend.util.JarUtils;
import com.github.tatercertified.asm_stripper.backend.util.StripProcessor;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.util.List;

/**
 * Configures the ASM Stripper environment
 */
public interface StripperPlugin {
    /**
     * If an AbstractNode should be stripped
     * @param node AbstractNode instance
     * @return True if it should be stripped
     */
    default boolean shouldStrip(AbstractNode node) {
        return node.shouldStrip();
    }

    /**
     * First entrypoint for ASM Stripper
     * @return a list of all ClassNodes
     */
    default List<ClassNode> init() {
        if (ASMStripper.VERBOSE) {
            System.out.println("Stripper Init");
        }
        JarUtils.setJarPathFromClass(this.getClass());
        try {
            return JarUtils.getClassNodes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Called before any stripping occurs
     */
    void preLoad();

    /**
     * Called before a strip has started
     * @param node AbstractNode that will be stripped
     * @param type The type of Strip
     */
    void preStrip(AbstractNode node, StripType type);

    /**
     * Call this in a static block in an early entrypoint.<p>
     * If this is in a Mixin environment, call this in a static block inside
     * your {@link org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin} instance
     */
    default void strip() {
        if (ASMStripper.VERBOSE) {
            System.out.println("Stripper Started");
        }
        StripProcessor.processStrips(this);
        if (ASMStripper.VERBOSE) {
            System.out.println("Stripper Ended");
        }
    }

    /**
     * Called after a strip has completed
     * @param node AbstractNode that was stripped
     * @param type The type of Strip
     */
    void postStrip(AbstractNode node, StripType type);

    /**
     * Called after the final strip has occurred
     */
    void onFinish();
}
