package com.vinnovateit.latch.desktop

import java.io.File
import java.io.RandomAccessFile
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import kotlin.concurrent.thread

/**
 * Prevents a second copy of Latch from running, and activates/unhides the existing
 * instance window when a second launch attempt is detected.
 *
 * Uses a file lock for process exclusion and a local loopback ServerSocket to
 * signal the existing process to raise its window.
 */
internal object SingleInstance {
    @Suppress("unused")
    private var lock: FileLock? = null
    private var channelRef: FileChannel? = null
    private var serverSocket: ServerSocket? = null

    private val portFile: File
        get() = AppPaths.dataDir.resolve(".port")

    /**
     * @param onActivate Callback invoked when a second instance tries to launch.
     * @return true if this process acquired the lock and may continue running.
     */
    fun acquire(onActivate: () -> Unit): Boolean {
        return try {
            val f = AppPaths.dataDir.resolve(".lock")
            f.parentFile?.mkdirs()
            val channel = RandomAccessFile(f, "rw").channel
            val acquired = runCatching { channel.tryLock() }.getOrNull()
            if (acquired == null) {
                runCatching { channel.close() }
                notifyRunningInstance()
                false
            } else {
                lock = acquired
                channelRef = channel
                startServer(onActivate)
                true
            }
        } catch (e: Throwable) {
            // If locking is impossible (odd filesystem, permissions), prefer
            // running over refusing to start.
            true
        }
    }

    private fun notifyRunningInstance() {
        runCatching {
            if (portFile.exists()) {
                val port = portFile.readText().trim().toIntOrNull() ?: return
                Socket(InetAddress.getByName("127.0.0.1"), port).use { socket ->
                    socket.getOutputStream().write("SHOW\n".toByteArray(Charsets.UTF_8))
                    socket.getOutputStream().flush()
                }
            }
        }
    }

    private fun startServer(onActivate: () -> Unit) {
        runCatching {
            val server = ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))
            serverSocket = server
            portFile.writeText(server.localPort.toString())

            thread(isDaemon = true, name = "SingleInstanceListener") {
                while (!server.isClosed) {
                    try {
                        val client = server.accept()
                        client.use {
                            val msg = it.getInputStream().bufferedReader().readLine()
                            if (msg == "SHOW") {
                                onActivate()
                            }
                        }
                    } catch (_: Throwable) {
                        break
                    }
                }
            }
        }
    }
}
