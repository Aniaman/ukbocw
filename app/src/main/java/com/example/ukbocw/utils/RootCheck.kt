package com.example.ukbocw.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootCheck {

    private const val WHICH_PROGRAM = "/system/xbin/which"
    private const val SU_PROGRAM = "su"
    private const val BUSY_BOX_PROGRAM = "busybox"

    private val binaryPaths = listOf(
        "/data/local/",
        "/data/local/bin/",
        "/data/local/xbin/",
        "/sbin/",
        "/su/bin/",
        "/system/bin/",
        "/system/bin/.ext/",
        "/system/bin/failsafe/",
        "/system/sd/xbin/",
        "/system/usr/we-need-root/",
        "/system/xbin/",
        "/system/app/Superuser.apk",
        "/cache",
        "/data",
        "/dev"
    )

    private val dangerousPackages = listOf(
        "com.devadvance.rootcloak",
        "com.devadvance.rootcloakplus",
        "com.koushikdutta.superuser",
        "com.thirdparty.superuser",
        "com.topjohnwu.magisk",
        "org.lsposed.manager",
        "com.devadvance.rootcloak2"
    )


    private fun detectTestKeys(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }


    private fun checkForBinary(filename: String): Boolean {
        for (path in binaryPaths) {
            val f = File(path, filename)
            val fileExists: Boolean = f.exists()
            if (fileExists) {
                return true
            }
        }
        return false
    }

    private fun checkForSuBinary(): Boolean = checkForBinary(SU_PROGRAM)

    private fun checkForBusyBoxBinary(): Boolean = checkForBinary(BUSY_BOX_PROGRAM)

    private fun checkSuExists(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime()
                .exec(arrayOf(WHICH_PROGRAM, SU_PROGRAM)) //Checking if su binary exists
            val bufferedReader = BufferedReader(InputStreamReader(process?.inputStream))
            val line = bufferedReader.readLine()
            process?.destroy()
            line != null
        } catch (e: Exception) {
            process?.destroy()
            false
        }
    }

    //Checking if dangerous applications are installed
    private fun checkPackages(context: Context): Boolean {
        for (name in dangerousPackages) {
            if (isPackageInstalled(name, context)) {
                return true
            }
        }
        return false
    }

    private fun isPackageInstalled(packageName: String, context: Context): Boolean = try {
        context.packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun enquireIsRooted(context: Context): DeviceRootState {
        val testCases = mutableListOf<String>()

        if (detectTestKeys()) {
            testCases.add("Found test-keys")
        }

        if (checkSuExists()) {
            testCases.add("Found `$SU_PROGRAM` in shell")
        }

        if (checkForBusyBoxBinary()) {
            testCases.add("Found `$BUSY_BOX_PROGRAM` binaries")
        }

        if (checkForSuBinary()) {
            testCases.add("Found `$SU_PROGRAM` binaries")
        }

        if (checkPackages(context)) {
            testCases.add("Found some of the `${dangerousPackages}` dangerous packages installed")
        }

        return if (testCases.isNotEmpty()) {
            DeviceRootState.Rooted(
                testReport = testCases.joinToString(
                    prefix = "deviceRootInfo: { ",
                    postfix = " }"
                )
            )
        } else {
            DeviceRootState.NotRooted
        }
    }
}

sealed interface DeviceRootState {
    object NotRooted : DeviceRootState
    data class Rooted(val testReport: String) : DeviceRootState
}