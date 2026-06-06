@file:Suppress("ktlint:standard:no-wildcard-imports")

package net.rsprox.patch.runelite

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
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

        val updateVanilla =
            cn.methods.singleOrNull {
                it.name == "updateVanilla" &&
                    it.desc == "(Lnet/runelite/client/rs/RSConfig;)V"
            }

        val patchedPort = updateVanilla?.let(::patchGamepackPort)
        if (patchedPort != null) {
            check(patchedPort) {
                "Could not patch gamepack request port"
            }
        }

        val cw = ClassWriter(0)
        cn.accept(cw)
        return cw.toByteArray()
    }

    private fun patchGamepackPort(method: MethodNode): Boolean {
        var patched = false

        for (insn in method.instructions.toArray()) {
            if (
                insn is MethodInsnNode &&
                insn.owner == "okhttp3/Request\$Builder" &&
                insn.name == "url" &&
                insn.desc == "(Lokhttp3/HttpUrl;)Lokhttp3/Request\$Builder;"
            ) {
                method.instructions.insertBefore(
                    insn,
                    InsnList().apply {
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "okhttp3/HttpUrl",
                                "newBuilder",
                                "()Lokhttp3/HttpUrl\$Builder;",
                                false,
                            ),
                        )
                        add(LdcInsnNode(43600))
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "okhttp3/HttpUrl\$Builder",
                                "port",
                                "(I)Lokhttp3/HttpUrl\$Builder;",
                                false,
                            ),
                        )
                        add(
                            MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                "okhttp3/HttpUrl\$Builder",
                                "build",
                                "()Lokhttp3/HttpUrl;",
                                false,
                            ),
                        )
                    },
                )

                patched = true
                break
            }
        }

        return patched
    }
}
