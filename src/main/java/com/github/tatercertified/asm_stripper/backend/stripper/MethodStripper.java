package com.github.tatercertified.asm_stripper.backend.stripper;

import com.github.tatercertified.asm_stripper.ASMStripper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class MethodStripper {
    /**
     * Removes a method in a class
     * @param node Method to remove
     * @param parent Parent class
     */
    public static void stripMethod(MethodNode node, ClassNode parent) {
        if (ASMStripper.VERBOSE) {
            System.out.println("Starting Stripping Method: " + node.name);
        }

        String internalName = parent.name.replace('.', '/');
        removeMethodUsages(parent, node.name, node.desc, internalName);

        if (ASMStripper.VERBOSE) {
            System.out.println("Finished Stripping Method: " + node.name);
        }
    }

    private static void removeMethodUsages(ClassNode classNode, String methodName, String methodDesc, String internalClassName) {
        for (MethodNode method : classNode.methods) {
            InsnList instructions = method.instructions;
            List<AbstractInsnNode> toRemove = new ArrayList<>();

            for (AbstractInsnNode insn : instructions) {
                if (insn instanceof MethodInsnNode methodInsn &&
                        methodInsn.owner.equals(internalClassName) &&
                        methodInsn.name.equals(methodName) &&
                        methodInsn.desc.equals(methodDesc)) {

                    if (ASMStripper.VERBOSE) {
                        System.out.println("Stripping method call in " + method.name + ": " + methodInsn.name + methodInsn.desc);
                    }

                    AbstractInsnNode start = insn.getPrevious();

                    int stackDepth = getStackEffect(methodInsn);
                    int insnLimit = 32;

                    while (start != null && insnLimit-- > 0 && stackDepth > 0) {
                        if (pushesToStack(start)) {
                            stackDepth--;
                        }
                        start = start.getPrevious();
                    }

                    // Collect all instructions from start to insn
                    AbstractInsnNode cur = (start != null) ? start.getNext() : instructions.getFirst();
                    while (cur != null && cur != insn.getNext()) {
                        toRemove.add(cur);
                        cur = cur.getNext();
                    }
                }
            }

            // Remove instructions
            for (AbstractInsnNode insn : toRemove) {
                instructions.remove(insn);
            }
        }
    }

    private static boolean pushesToStack(AbstractInsnNode insn) {
        // Heuristic: these instructions likely push something to the stack
        return insn instanceof InsnNode ||
                insn instanceof IntInsnNode ||
                insn instanceof LdcInsnNode ||
                insn instanceof VarInsnNode ||
                insn instanceof TypeInsnNode ||
                insn instanceof MethodInsnNode;
    }

    private static int getStackEffect(MethodInsnNode methodInsn) {
        // Calculate how many arguments this method expects to pop off the stack
        int argCount = Type.getArgumentTypes(methodInsn.desc).length;
        return methodInsn.getOpcode() == Opcodes.INVOKESTATIC ? argCount : argCount + 1;
    }
}
