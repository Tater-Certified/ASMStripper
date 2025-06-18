package com.github.tatercertified.asm_stripper.backend.stripper;

import com.github.tatercertified.asm_stripper.ASMStripper;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class ClassStripper {
    /**
     * Removes the specified class and references to it
     * @param targetClass Class to remove
     * @param allClasses Classes where the stripped class is referenced
     */
    public static void stripClass(ClassNode targetClass, List<ClassNode> allClasses) {
        if (ASMStripper.VERBOSE) {
            System.out.println("Stripping Class: " + targetClass.name);
        }

        // Remove all references to this class in other classes
        for (ClassNode classNode : allClasses) {
            removeTypeUsages(classNode, targetClass.name);
        }

        if (ASMStripper.VERBOSE) {
            System.out.println("Finished Stripping Class: " + targetClass.name);
        }
    }

    private static void removeTypeUsages(ClassNode classNode, String targetInternalName) {
        // Remove fields of the target type
        classNode.fields.removeIf(field -> field.desc.contains("L" + targetInternalName + ";"));

        for (MethodNode method : classNode.methods) {
            InsnList instructions = method.instructions;
            List<AbstractInsnNode> toRemove = new ArrayList<>();

            for (AbstractInsnNode insn : instructions) {
                if (insn instanceof TypeInsnNode typeInsn) {
                    if (typeInsn.desc.equals(targetInternalName)) {
                        if (ASMStripper.VERBOSE) {
                            System.out.println("Removing type usage in method " + method.name + ": " + typeInsn.desc);
                        }
                        toRemove.add(typeInsn);
                    }
                } else if (insn instanceof MethodInsnNode methodInsn) {
                    if (methodInsn.owner.equals(targetInternalName)) {
                        if (ASMStripper.VERBOSE) {
                            System.out.println("Removing method call to " + methodInsn.owner + " in method " + method.name);
                        }
                        toRemove.add(methodInsn);
                    }
                } else if (insn instanceof FieldInsnNode fieldInsn) {
                    if (fieldInsn.owner.equals(targetInternalName)) {
                        if (ASMStripper.VERBOSE) {
                            System.out.println("Removing field access to " + fieldInsn.owner + " in method " + method.name);
                        }
                        toRemove.add(fieldInsn);
                    }
                }
            }

            // Remove instructions
            for (AbstractInsnNode insn : toRemove) {
                instructions.remove(insn);
            }
        }
    }
}
