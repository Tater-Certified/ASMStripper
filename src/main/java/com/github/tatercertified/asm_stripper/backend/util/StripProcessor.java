package com.github.tatercertified.asm_stripper.backend.util;

import com.github.tatercertified.asm_stripper.ASMStripper;
import com.github.tatercertified.asm_stripper.api.StripType;
import com.github.tatercertified.asm_stripper.api.StripperPlugin;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.List;
import java.util.ListIterator;

public final class StripProcessor {
    /**
     * Processes the Strip Annotations
     * @param plugin StripperPlugin instance
     */
    public static void processStrips(StripperPlugin plugin) {
        // Init
        List<ClassNode> allNodes = plugin.init();

        // Preload
        if (ASMStripper.VERBOSE) {
            System.out.println("Stripper Preload");
        }
        plugin.preLoad();

        // Check classes
        ListIterator<ClassNode> classNodes = allNodes.listIterator();
        while (classNodes.hasNext()) {
            ClassNode node = classNodes.next();
            AbstractNode abstractNode = AbstractNode.from(node);
            if (ASMStripper.VERBOSE) {
                System.out.println("Checking " + abstractNode.getName());
            }
            if (abstractNode.isStrippable()) {
                if (ASMStripper.VERBOSE) {
                    System.out.println(abstractNode.getName() + " is Strippable");
                }
                if (plugin.shouldStrip(abstractNode)) {
                    if (ASMStripper.VERBOSE) {
                        System.out.println(abstractNode.getName() + " is Stripping");
                    }
                    plugin.preStrip(abstractNode, StripType.Class);
                    abstractNode.strip();
                    plugin.postStrip(abstractNode, StripType.Class);
                    classNodes.remove();
                    // Skip all the other stuff in the class
                    continue;
                }
            }

            // Check Methods
            ListIterator<MethodNode> methodNodes = node.methods.listIterator();
            while (methodNodes.hasNext()) {
                MethodNode methodNode = methodNodes.next();
                AbstractNode abstractMethodNode = AbstractNode.from(methodNode, node);
                if (ASMStripper.VERBOSE) {
                    System.out.println("Checking " + abstractMethodNode.getName());
                }
                if (plugin.shouldStrip(abstractMethodNode)) {
                    if (ASMStripper.VERBOSE) {
                        System.out.println(abstractMethodNode.getName() + " is Stripping");
                    }
                    plugin.preStrip(abstractMethodNode, StripType.Method);
                    abstractMethodNode.strip();
                    plugin.postStrip(abstractMethodNode, StripType.Method);
                    methodNodes.remove();
                }
            }

            // Check Fields
            ListIterator<FieldNode> fieldNodes = node.fields.listIterator();
            while (fieldNodes.hasNext()) {
                FieldNode fieldNode = fieldNodes.next();
                AbstractNode abstractFieldNode = AbstractNode.from(fieldNode, node);
                if (ASMStripper.VERBOSE) {
                    System.out.println("Checking " + abstractFieldNode.getName());
                }
                if (plugin.shouldStrip(abstractFieldNode)) {
                    if (ASMStripper.VERBOSE) {
                        System.out.println(abstractFieldNode.getName() + " is Stripping");
                    }
                    plugin.preStrip(abstractFieldNode, StripType.Field);
                    abstractFieldNode.strip();
                    plugin.postStrip(abstractFieldNode, StripType.Field);
                    fieldNodes.remove();
                }
            }
        }

        // On Finish
        if (ASMStripper.VERBOSE) {
            System.out.println("Stripper OnFinish");
        }
        plugin.onFinish();
    }
}
