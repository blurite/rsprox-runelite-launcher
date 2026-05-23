@file:Suppress("ktlint:standard:no-wildcard-imports")

package net.rsprox.patch.runelite

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*

public class WorldClientPatcher : ClassPatcher {
    override fun patch(classFile: ByteArray): ByteArray {
        val cr = ClassReader(classFile)
        val cn = ClassNode()
        cr.accept(cn, 0)

        check(cn.name == "net/runelite/client/game/WorldClient")

        val method =
            cn.methods.single {
                it.name == "lookupWorlds" &&
                    it.desc == "()Lnet/runelite/http/api/worlds/WorldResult;"
            }

        val insns = method.instructions

        for (insn in insns.toArray()) {
            if (insn.opcode != ALOAD) continue
            val aload = insn as VarInsnNode
            if (aload.`var` != 0) continue

            val getField = aload.next ?: continue
            if (getField.opcode != GETFIELD) continue
            getField as FieldInsnNode

            if (getField.owner != "net/runelite/client/game/WorldClient" ||
                getField.name != "apiBase" ||
                getField.desc != "Lokhttp3/HttpUrl;"
            ) {
                continue
            }

            val newBuilder = getField.next ?: continue
            if (newBuilder.opcode != INVOKEVIRTUAL) continue
            newBuilder as MethodInsnNode

            if (newBuilder.owner != "okhttp3/HttpUrl" ||
                newBuilder.name != "newBuilder" ||
                newBuilder.desc != "()Lokhttp3/HttpUrl\$Builder;"
            ) {
                continue
            }

            val replacement =
                InsnList().apply {
                    add(TypeInsnNode(NEW, "okhttp3/HttpUrl\$Builder"))
                    add(InsnNode(DUP))
                    add(
                        MethodInsnNode(
                            INVOKESPECIAL,
                            "okhttp3/HttpUrl\$Builder",
                            "<init>",
                            "()V",
                            false,
                        ),
                    )

                    add(LdcInsnNode("http"))
                    add(
                        MethodInsnNode(
                            INVOKEVIRTUAL,
                            "okhttp3/HttpUrl\$Builder",
                            "scheme",
                            "(Ljava/lang/String;)Lokhttp3/HttpUrl\$Builder;",
                            false,
                        ),
                    )

                    add(LdcInsnNode("127.1.45.2"))
                    add(
                        MethodInsnNode(
                            INVOKEVIRTUAL,
                            "okhttp3/HttpUrl\$Builder",
                            "host",
                            "(Ljava/lang/String;)Lokhttp3/HttpUrl\$Builder;",
                            false,
                        ),
                    )

                    add(LdcInsnNode(43600))
                    add(
                        MethodInsnNode(
                            INVOKEVIRTUAL,
                            "okhttp3/HttpUrl\$Builder",
                            "port",
                            "(I)Lokhttp3/HttpUrl\$Builder;",
                            false,
                        ),
                    )
                }

            insns.insertBefore(aload, replacement)

            insns.remove(aload)
            insns.remove(getField)
            insns.remove(newBuilder)

            break
        }

        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        cn.accept(cw)
        return cw.toByteArray()
    }
}
