package com.example.classroomseating

import android.app.Application
import android.os.Build
import android.os.Process
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltAndroidApp
class ClassRoomSeatingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashLogger()
    }

    /**
     * Записывает все необработанные исключения в [CRASH_LOG_FILE] внутри filesDir.
     * Позволяет диагностировать краши без подключённого устройства.
     * После записи передаёт исключение прежнему обработчику (штатное закрытие).
     */
    private fun installCrashLogger() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        val appDir = filesDir
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val file = File(appDir, CRASH_LOG_FILE)
                val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                val info = StringBuilder()
                info.append("==== CRASH ").append(stamp).append(" ====\n")
                info.append("thread=").append(thread.name).append("\n")
                info.append("pid=").append(Process.myPid())
                info.append(" sdk=").append(Build.VERSION.SDK_INT)
                info.append(" model=").append(Build.MODEL).append("\n")
                info.append(throwable.javaClass.name).append(": ").append(throwable.message).append("\n")
                for (line in throwable.stackTraceStringLines()) {
                    info.append("    at ").append(line).append("\n")
                }
                info.append("\n")
                file.appendText(info.toString())
            } catch (_: Throwable) {
                // Ничего не делаем — логгер не должен сам уронить приложение.
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun Throwable.stackTraceStringLines(): List<String> {
        val writer = java.io.StringWriter()
        printStackTrace(java.io.PrintWriter(writer))
        return writer.toString().lineSequence().toList()
    }

    companion object {
        const val CRASH_LOG_FILE = "classroom_crashes.log"
    }
}