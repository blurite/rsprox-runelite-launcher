@file:Suppress("ktlint:standard:no-wildcard-imports")

package net.rsprox.patch.runelite

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.*

public class ClientLoaderPatcher : ClassPatcher {
    override fun patch(classFile: ByteArray): ByteArray {
        val cr = ClassReader(classFile)
        val cn = ClassNode()
        cr.accept(cn, 0)

        check(cn.name == "net/runelite/client/rs/ClientLoader")

        val method =
            cn.methods.single {
                it.name == "downloadConfig" &&
                    it.desc == "()Lnet/runelite/client/rs/RSConfig;"
            }

        var patched = false

        for (insn in method.instructions.toArray()) {
            if (insn is LdcInsnNode && insn.cst == ".jagex.com") {
                insn.cst = ""
                patched = true
                break
            }
        }

        check(patched) {
            "Could not find .jagex.com host check constant"
        }

        val cw = ClassWriter(0)
        cn.accept(cw)
        return cw.toByteArray()
    }
}
