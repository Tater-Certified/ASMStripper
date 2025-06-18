package com.github.tatercertified.asm_stripper.backend.stripper;

import com.github.tatercertified.asm_stripper.ASMStripper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;

public class FieldStripper {
    /**
     * Removes a field from a class
     * @param node Field to remove
     * @param parent Parent class
     */
    public static void stripField(FieldNode node, ClassNode parent) {
        if (ASMStripper.VERBOSE) {
            System.out.println("Beginning Stripping Field: " + node.name);
        }

        String internalName = parent.name.replace('.', '/');
        stripStaticFieldInitializers(parent, node.desc, internalName);
        removeStaticFieldUsagesInClinit(parent, node.desc, internalName);

        if (ASMStripper.VERBOSE) {
            System.out.println("Finished Stripping Field: " + node.name);
        }
    }

    private static void stripStaticFieldInitializers(ClassNode classNode, String fieldDesc, String internalClassName) {
        for (MethodNode method : classNode.methods) {
            if (!method.name.equals("<clinit>")) {
                continue;
            }

            InsnList instructions = method.instructions;
            List<AbstractInsnNode> toRemove = new ArrayList<>();

            for (AbstractInsnNode insn : instructions) {
                if (insn instanceof FieldInsnNode fieldInsn &&
                        fieldInsn.getOpcode() == Opcodes.PUTSTATIC &&
                        fieldInsn.owner.equals(internalClassName) &&
                        fieldDesc.equals(fieldInsn.desc)) {

                    if (ASMStripper.VERBOSE) {
                        System.out.println("Stripping static field initializer: " + fieldInsn.name);
                    }

                    AbstractInsnNode start = insn.getPrevious();

                    int stackDepth = 1;
                    int insnLimit = 32;
                    while (start != null && insnLimit-- > 0) {
                        if (start instanceof InsnNode || start instanceof MethodInsnNode ||
                                start instanceof LdcInsnNode || start instanceof IntInsnNode ||
                                start instanceof VarInsnNode || start instanceof TypeInsnNode) {
                            // Heuristic: this might push to the stack
                            stackDepth--;
                        }

                        if (stackDepth == 0) {
                            break;
                        }

                        start = start.getPrevious();
                    }

                    // Collect all instructions from start to end
                    AbstractInsnNode cur = (start != null) ? start.getNext() : instructions.getFirst();
                    while (cur != null && cur != insn.getNext()) {
                        toRemove.add(cur);
                        cur = cur.getNext();
                    }
                    break;
                }
            }

            for (AbstractInsnNode insn : toRemove) {
                instructions.remove(insn);
            }

            // Ensure <clinit> ends correctly
            AbstractInsnNode last = instructions.getLast();
            if (last == null || last.getOpcode() != Opcodes.RETURN) {
                instructions.add(new InsnNode(Opcodes.RETURN));
            }
        }
    }

    private static void removeStaticFieldUsagesInClinit(ClassNode classNode, String fieldDesc, String internalName) {
        for (MethodNode method : classNode.methods) {
            if (!method.name.equals("<clinit>")) continue;

            InsnList insns = method.instructions;

            for (AbstractInsnNode insn : insns) {
                if (insn instanceof FieldInsnNode fieldInsn &&
                        fieldInsn.owner.equals(internalName) &&
                        fieldDesc.equals(fieldInsn.desc)) {

                    List<AbstractInsnNode> toRemove = new ArrayList<>();

                    if (fieldInsn.getOpcode() == Opcodes.GETSTATIC) {
                        // Possible read and use — try to find an array store like IASTORE, DASTORE, etc.
                        AbstractInsnNode current = insn;
                        toRemove.add(current);
                        for (int j = 0; j < 6 && current != null; j++) {
                            current = current.getNext();
                            if (current != null) toRemove.add(current);
                            if (current instanceof InsnNode) {
                                int op = current.getOpcode();
                                if (op >= Opcodes.IASTORE && op <= Opcodes.SASTORE) {
                                    break;
                                }
                            }
                        }
                    } else if (fieldInsn.getOpcode() == Opcodes.PUTSTATIC) {
                        // Assigning to the field, remove backward up to 3 or so instructions, and the PUTSTATIC
                        toRemove.add(fieldInsn);
                        AbstractInsnNode prev = fieldInsn.getPrevious();
                        for (int j = 0; j < 3 && prev != null; j++) {
                            toRemove.addFirst(prev);
                            prev = prev.getPrevious();
                        }
                    }

                    if (ASMStripper.VERBOSE) {
                        System.out.println("Removing usage of static field: " + fieldInsn.name);
                    }
                    for (AbstractInsnNode toDel : toRemove) {
                        insns.remove(toDel);
                    }

                    break;
                }
            }
        }
    }
}
