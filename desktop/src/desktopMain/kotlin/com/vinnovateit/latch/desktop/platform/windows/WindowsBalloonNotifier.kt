package com.vinnovateit.latch.desktop.platform.windows

import com.sun.jna.Native
import com.sun.jna.Structure
import com.sun.jna.platform.win32.Guid
import com.sun.jna.platform.win32.Kernel32
import com.sun.jna.platform.win32.Shell32
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HICON
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.StdCallLibrary
import com.vinnovateit.latch.core.platform.Logger
import com.vinnovateit.latch.desktop.AppPaths
import com.vinnovateit.latch.desktop.platform.InstalledBuild
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal object WindowsBalloonNotifier {

    private const val NIM_ADD = 0
    private const val NIM_MODIFY = 1
    private const val NIM_DELETE = 2

    private const val ICON_LINGER_MS = 8_000L

    private const val NIF_ICON = 0x00000002
    private const val NIF_INFO = 0x00000010

    private const val NIIF_ERROR = 0x00000003
    private const val NIIF_USER = 0x00000004
    private const val NIIF_NOSOUND = 0x00000010
    private const val NIIF_LARGE_ICON = 0x00000020

    private const val IMAGE_ICON = 1
    private const val LR_LOADFROMFILE = 0x00000010

    private const val ICON_UID = 0xB1A7

    private val hwnd = AtomicReference<HWND?>()
    private val hIcon = AtomicReference<HICON?>()
    private val ready = CountDownLatch(1)
    private val started = AtomicBoolean(false)
    private val iconAdded = AtomicBoolean(false)
    private val generation = AtomicLong(0)
    private var logger: Logger? = null

    private const val TAG = "WindowsBalloonNotifier"

    @Volatile private var wndProc: WinUser.WindowProc? = null

    fun start(logger: Logger) {
        this.logger = logger
        if (!started.compareAndSet(false, true)) return
        val t = Thread(::threadMain, "latch-notify")
        t.isDaemon = true
        t.start()
    }

    fun notify(title: String, message: String, isError: Boolean) {
        ready.await()
        val win = hwnd.get()
        val icon = hIcon.get()
        logger?.d(TAG, "notify: win=$win icon=$icon title='$title' text='$message'")
        if (win == null || icon == null) {
            logger?.w(TAG, "notify: aborting, tray icon not initialised (win=$win, icon=$icon)")
            return
        }

        val nid = NOTIFYICONDATA().apply {
            cbSize = size()
            hWnd = win
            uID = ICON_UID
            uFlags = NIF_INFO or NIF_ICON
            hIcon = icon
            szInfoTitle = title.take(63).toWideChars(64)
            szInfo = message.take(255).toWideChars(256)
            dwInfoFlags = if (isError) {
                NIIF_ERROR or NIIF_NOSOUND
            } else {
                NIIF_USER or NIIF_LARGE_ICON or NIIF_NOSOUND
            }
            hBalloonIcon = icon
        }
        val op = if (iconAdded.compareAndSet(false, true)) NIM_ADD else NIM_MODIFY
        val ok = Shell32Ext.INSTANCE.Shell_NotifyIconW(op, nid)
        if (!ok) {
            logger?.w(TAG, "Shell_NotifyIconW($op) returned FALSE, last error=${Native.getLastError()}")
            iconAdded.set(false)
            return
        }
        scheduleRemoval(win)
    }

    private fun scheduleRemoval(win: HWND) {
        val myGeneration = generation.incrementAndGet()
        val t = Thread({
            Thread.sleep(ICON_LINGER_MS)
            if (generation.get() != myGeneration) return@Thread
            if (!iconAdded.compareAndSet(true, false)) return@Thread
            val nid = NOTIFYICONDATA().apply {
                cbSize = size()
                hWnd = win
                uID = ICON_UID
            }
            Shell32Ext.INSTANCE.Shell_NotifyIconW(NIM_DELETE, nid)
        }, "latch-notify-linger")
        t.isDaemon = true
        t.start()
    }

    private fun threadMain() {
        try {
            val className = "LatchNotifyWnd"
            val hInstance = Kernel32.INSTANCE.GetModuleHandle(null)

            val proc = WinUser.WindowProc { h, msg, wParam, lParam ->
                User32.INSTANCE.DefWindowProc(h, msg, wParam, lParam)
            }
            wndProc = proc

            val wc = WinUser.WNDCLASSEX()
            wc.lpszClassName = className
            wc.lpfnWndProc = proc
            wc.hInstance = hInstance
            val atom = User32.INSTANCE.RegisterClassEx(wc)
            logger?.d(TAG, "RegisterClassEx -> atom=${atom.toInt()} lastError=${Native.getLastError()}")
            if (atom.toInt() == 0) return

            val win = User32.INSTANCE.CreateWindowEx(
                0, className, "Latch Notify", 0,
                0, 0, 0, 0,
                null, null, hInstance, null,
            )
            hwnd.set(win)
            logger?.d(TAG, "CreateWindowEx -> hwnd=$win lastError=${Native.getLastError()}")
            if (win == null) return

            val icon = loadIcon()
            hIcon.set(icon)
            logger?.d(TAG, "loadIcon -> $icon")
        } finally {
            ready.countDown()
        }

        val msg = WinUser.MSG()
        while (User32.INSTANCE.GetMessage(msg, null, 0, 0) != 0) {
            User32.INSTANCE.TranslateMessage(msg)
            User32.INSTANCE.DispatchMessage(msg)
        }
    }

    private fun loadIcon(): HICON? {
        val exePath = InstalledBuild.path
        logger?.d(TAG, "loadIcon: InstalledBuild.path=$exePath")
        if (exePath != null) {
            val large = arrayOfNulls<HICON>(1)
            val count = Shell32.INSTANCE.ExtractIconEx(exePath, 0, large, null, 1)
            logger?.d(TAG, "loadIcon: ExtractIconEx($exePath) -> count=$count icon=${large[0]}")
            if (count > 0 && large[0] != null) return large[0]
        }

        val icoFile = File(AppPaths.dataDir, "latch.ico")
        if (!icoFile.exists()) {
            val extracted = runCatching {
                WindowsBalloonNotifier::class.java.classLoader
                    .getResourceAsStream("latch.ico")
                    ?.use { it.copyTo(icoFile.outputStream()) }
            }
            logger?.d(TAG, "loadIcon: extracted latch.ico from classpath -> $extracted (exists=${icoFile.exists()})")
        }
        if (!icoFile.exists()) {
            logger?.w(TAG, "loadIcon: no icon source available at ${icoFile.absolutePath}")
            return null
        }

        val handle = User32.INSTANCE.LoadImage(
            null, icoFile.absolutePath, IMAGE_ICON, 64, 64, LR_LOADFROMFILE,
        )
        val loaded = handle?.let { HICON(it) }
        logger?.d(TAG, "loadIcon: LoadImage(${icoFile.absolutePath}) -> $loaded lastError=${Native.getLastError()}")
        return loaded
    }

    private fun String.toWideChars(size: Int): CharArray =
        toCharArray().copyOf(size)

    class NOTIFYICONDATA : Structure() {
        @JvmField var cbSize: Int = 0
        @JvmField var hWnd: HWND? = null
        @JvmField var uID: Int = 0
        @JvmField var uFlags: Int = 0
        @JvmField var uCallbackMessage: Int = 0
        @JvmField var hIcon: HICON? = null
        @JvmField var szTip: CharArray = CharArray(128)
        @JvmField var dwState: Int = 0
        @JvmField var dwStateMask: Int = 0
        @JvmField var szInfo: CharArray = CharArray(256)
        @JvmField var uVersion: Int = 0
        @JvmField var szInfoTitle: CharArray = CharArray(64)
        @JvmField var dwInfoFlags: Int = 0
        @JvmField var guidItem: Guid.GUID = Guid.GUID()
        @JvmField var hBalloonIcon: HICON? = null

        override fun getFieldOrder(): List<String> = listOf(
            "cbSize", "hWnd", "uID", "uFlags", "uCallbackMessage", "hIcon",
            "szTip", "dwState", "dwStateMask", "szInfo", "uVersion", "szInfoTitle",
            "dwInfoFlags", "guidItem", "hBalloonIcon",
        )
    }

    interface Shell32Ext : StdCallLibrary {
        fun Shell_NotifyIconW(dwMessage: Int, lpData: NOTIFYICONDATA): Boolean

        companion object {
            val INSTANCE: Shell32Ext = Native.load("shell32", Shell32Ext::class.java)
        }
    }
}
