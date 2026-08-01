package com.vinnovateit.latch.desktop

import java.io.RandomAccessFile
import java.nio.channels.FileLock

/**
 * Prevents a second copy of Latch from running.
 *
 * This is mandatory rather than a nicety: the app starts automatically at login,
 * so a user double-clicking the shortcut would otherwise get two [LatchEngine]s,
 * two health-check loops, and two credential-bearing POSTs racing each other at
 * the portal.
 *
 * Uses a file lock rather than a ServerSocket on purpose -- binding a socket
 * triggers a Windows Firewall prompt on first run.
 */
internal object SingleInstance {
    // Held for process lifetime; releasing it would defeat the guard.
    @Suppress("unused")
    private var lock: FileLock? = null
    private var channelRef: java.nio.channels.FileChannel? = null

    /** @return true if this process acquired the lock and may continue. */
    fun acquire(): Boolean {
        return try {
            val f = AppPaths.dataDir.resolve(".lock")
            f.parentFile?.mkdirs()
            val channel = RandomAccessFile(f, "rw").channel
            val acquired = runCatching { channel.tryLock() }.getOrNull()
            if (acquired == null) {
                runCatching { channel.close() }
                false
            } else {
                lock = acquired
                channelRef = channel
                true
            }
        } catch (e: Throwable) {
            // If locking is impossible (odd filesystem, permissions), prefer
            // running over refusing to start.
            true
        }
    }
}
