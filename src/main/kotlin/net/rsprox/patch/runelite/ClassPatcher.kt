package net.rsprox.patch.runelite

public interface ClassPatcher {
    public fun patch(classFile: ByteArray): ByteArray
}
