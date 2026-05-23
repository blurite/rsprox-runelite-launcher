@file:Suppress("ktlint:standard:no-wildcard-imports")

package net.rsprox.patch.runelite

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

public class DeveloperToolsPatcher : ClassPatcher {
    override fun patch(classFile: ByteArray): ByteArray {
        val cr = ClassReader(classFile)
        val cn = ClassNode()
        cr.accept(cn, 0)

        check(cn.name == "net/runelite/client/RuneLite")

        val method =
            cn.methods.single {
                it.name == "main" && it.desc == "([Ljava/lang/String;)V"
            }

        val insns = method.instructions
        val all = insns.toArray()

        val devModeHas =
            all.firstOrNull { insn ->
                insn.opcode == ALOAD &&
                    insn.next?.opcode == LDC &&
                    (insn.next as LdcInsnNode).cst == "developer-mode" &&
                    insn.next.next?.opcode == INVOKEVIRTUAL &&
                    (insn.next.next as MethodInsnNode).owner == "joptsimple/OptionSet" &&
                    (insn.next.next as MethodInsnNode).name == "has" &&
                    (insn.next.next as MethodInsnNode).desc == "(Ljava/lang/String;)Z"
            } ?: error("Could not find developer-mode check")

        val developerModeStore =
            generateSequence(devModeHas) { it.next }
                .firstOrNull { it.opcode == ISTORE }
                ?: error("Could not find developerMode store")

        val launcherVersionCall =
            generateSequence(devModeHas) { it.next }
                .takeWhile { it !== developerModeStore }
                .firstOrNull { insn ->
                    insn.opcode == INVOKESTATIC &&
                        (insn as MethodInsnNode).owner == "net/runelite/client/RuneLiteProperties" &&
                        insn.name == "getLauncherVersion" &&
                        insn.desc == "()Ljava/lang/String;"
                } ?: error("Could not find getLauncherVersion() inside developer-mode check")

        insns.set(launcherVersionCall, InsnNode(ACONST_NULL))

        val assertionsDisabledRead =
            generateSequence(developerModeStore.next) { it.next }
                .firstOrNull { insn ->
                    insn.opcode == GETSTATIC &&
                        (insn as FieldInsnNode).owner == "net/runelite/client/RuneLite" &&
                        insn.name == "\$assertionsDisabled" &&
                        insn.desc == "Z"
                } ?: error("Could not find \$assertionsDisabled read")

        insns.set(assertionsDisabledRead, InsnNode(ICONST_0))

        val cw = ClassWriter(0)
        cn.accept(cw)
        return cw.toByteArray()
    }
}
