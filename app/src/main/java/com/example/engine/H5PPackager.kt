package com.example.engine

import android.content.Context
import com.example.data.db.LibraryDao
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import com.example.data.model.H5PLibraryEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class H5PPackager(
    private val context: Context,
    private val libraryDao: LibraryDao
) {
    private val librariesDir: File = File(context.filesDir, "h5p_libraries").apply {
        if (!exists()) mkdirs()
    }

    private val exportsDir: File = File(context.filesDir, "exports").apply {
        if (!exists()) mkdirs()
    }

    /**
     * Harvests all libraries found inside an .h5p / .zip archive into local storage
     * and registers them in the Room database.
     */
    suspend fun harvestLibrariesFromStream(inputStream: InputStream): Int {
        var harvestedCount = 0
        val zip = ZipInputStream(BufferedInputStream(inputStream))
        var entry: ZipEntry? = zip.nextEntry
        val tempExtractDir = File(context.cacheDir, "h5p_harvest_temp_${System.currentTimeMillis()}").apply { mkdirs() }

        try {
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && !name.startsWith("__MACOSX")) {
                    val destFile = File(tempExtractDir, name)
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { out ->
                        zip.copyTo(out)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            // Inspect directories in tempExtractDir
            tempExtractDir.walkTopDown().forEach { file ->
                if (file.name == "library.json") {
                    val libDir = file.parentFile ?: return@forEach
                    val jsonStr = file.readText()
                    val libEntity = parseLibraryJson(jsonStr, libDir.name)
                    if (libEntity != null) {
                        // Copy entire library directory to persistent storage
                        val targetLibDir = File(librariesDir, libDir.name)
                        if (!targetLibDir.exists()) targetLibDir.mkdirs()
                        libDir.copyRecursively(targetLibDir, overwrite = true)

                        val persistentEntity = libEntity.copy(
                            sourcePath = targetLibDir.absolutePath,
                            isPrepackaged = false
                        )
                        libraryDao.insertLibrary(persistentEntity)
                        harvestedCount++
                    }
                }
            }
        } finally {
            tempExtractDir.deleteRecursively()
        }
        return harvestedCount
    }

    /**
     * Imports a full .h5p package: extracts its libraries, extracts its content.json
     * and h5p.json, and returns a ready ActivityEntity.
     */
    suspend fun importPackage(inputStream: InputStream, filenameHint: String): Pair<ActivityEntity, Int> {
        val tempExtractDir = File(context.cacheDir, "h5p_import_temp_${System.currentTimeMillis()}").apply { mkdirs() }
        var h5pJsonStr = ""
        var contentJsonStr = ""
        var harvestedCount = 0

        try {
            val zip = ZipInputStream(BufferedInputStream(inputStream))
            var entry: ZipEntry? = zip.nextEntry

            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && !name.startsWith("__MACOSX")) {
                    val destFile = File(tempExtractDir, name)
                    destFile.parentFile?.mkdirs()
                    FileOutputStream(destFile).use { out ->
                        zip.copyTo(out)
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }

            // Read h5p.json
            val h5pFile = File(tempExtractDir, "h5p.json")
            if (h5pFile.exists()) {
                h5pJsonStr = h5pFile.readText()
            }

            // Read content/content.json
            val contentFile = File(tempExtractDir, "content/content.json")
            if (contentFile.exists()) {
                contentJsonStr = contentFile.readText()
            } else {
                // Check if content.json is in root
                val rootContent = File(tempExtractDir, "content.json")
                if (rootContent.exists()) contentJsonStr = rootContent.readText()
            }

            // Harvest all libraries in the imported package
            tempExtractDir.walkTopDown().forEach { file ->
                if (file.name == "library.json") {
                    val libDir = file.parentFile ?: return@forEach
                    val jsonStr = file.readText()
                    val libEntity = parseLibraryJson(jsonStr, libDir.name)
                    if (libEntity != null) {
                        val targetLibDir = File(librariesDir, libDir.name)
                        if (!targetLibDir.exists()) targetLibDir.mkdirs()
                        libDir.copyRecursively(targetLibDir, overwrite = true)

                        val persistentEntity = libEntity.copy(
                            sourcePath = targetLibDir.absolutePath,
                            isPrepackaged = false
                        )
                        libraryDao.insertLibrary(persistentEntity)
                        harvestedCount++
                    }
                }
            }

            val manifestObj = if (h5pJsonStr.isNotBlank()) JSONObject(h5pJsonStr) else JSONObject()
            val title = manifestObj.optString("title", filenameHint.replace(".h5p", "").replace(".zip", ""))
            val mainLib = manifestObj.optString("mainLibrary", "H5P.MultiChoice")
            
            val type = when {
                mainLib.contains("MultiChoice", ignoreCase = true) -> ActivityType.MULTI_CHOICE
                mainLib.contains("Dialogcards", ignoreCase = true) -> ActivityType.FLASHCARDS
                mainLib.contains("Blanks", ignoreCase = true) -> ActivityType.BLANKS
                else -> ActivityType.IMPORTED
            }

            val activity = ActivityEntity(
                id = UUID.randomUUID().toString(),
                title = title,
                description = "Imported H5P package ($mainLib)",
                type = type,
                mainLibrary = mainLib,
                libraryVersion = manifestObj.optString("majorVersion", "1") + "." + manifestObj.optString("minorVersion", "0"),
                contentJson = if (contentJsonStr.isNotBlank()) contentJsonStr else "{}",
                h5pJson = if (h5pJsonStr.isNotBlank()) h5pJsonStr else "{\"title\":\"$title\",\"mainLibrary\":\"$mainLib\"}",
                isPreloaded = false,
                bestScore = 0,
                maxScore = 0
            )

            return Pair(activity, harvestedCount)
        } finally {
            tempExtractDir.deleteRecursively()
        }
    }

    /**
     * Exports a native ActivityEntity as a standard, portable .h5p ZIP package
     * bundled with h5p.json, content/content.json, and all required library folders.
     */
    fun exportActivityToH5P(activity: ActivityEntity): File {
        val sanitizedTitle = activity.title.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val outFile = File(exportsDir, "$sanitizedTitle.h5p")
        if (outFile.exists()) outFile.delete()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outFile))).use { zipOut ->
            // 1. Write h5p.json
            val h5pEntry = ZipEntry("h5p.json")
            zipOut.putNextEntry(h5pEntry)
            zipOut.write(activity.h5pJson.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 2. Write content/content.json
            val contentEntry = ZipEntry("content/content.json")
            zipOut.putNextEntry(contentEntry)
            zipOut.write(activity.contentJson.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 3. Package required libraries from assets or harvested files
            val manifestObj = try { JSONObject(activity.h5pJson) } catch (e: Exception) { JSONObject() }
            val depsArray = manifestObj.optJSONArray("preloadedDependencies") ?: JSONArray()
            val libsToInclude = mutableSetOf<String>()

            val mainLib = activity.mainLibrary
            val mainVer = activity.libraryVersion
            if (mainLib.isNotBlank()) {
                libsToInclude.add("$mainLib-$mainVer")
            }

            for (i in 0 until depsArray.length()) {
                val dep = depsArray.getJSONObject(i)
                val mName = dep.optString("machineName")
                val maj = dep.optInt("majorVersion")
                val min = dep.optInt("minorVersion")
                if (mName.isNotBlank()) {
                    libsToInclude.add("$mName-$maj.$min")
                }
            }

            // Write library folders
            libsToInclude.forEach { libDirName ->
                // Check in harvested libraries first
                val harvestedLibDir = File(librariesDir, libDirName)
                if (harvestedLibDir.exists() && harvestedLibDir.isDirectory) {
                    harvestedLibDir.walkTopDown().forEach { file ->
                        if (!file.isDirectory) {
                            val relativePath = "$libDirName/" + file.relativeTo(harvestedLibDir).path
                            val entry = ZipEntry(relativePath)
                            zipOut.putNextEntry(entry)
                            file.inputStream().use { it.copyTo(zipOut) }
                            zipOut.closeEntry()
                        }
                    }
                } else {
                    // Check in bundled assets/h5p/libraries/
                    try {
                        val assetPath = "h5p/libraries/$libDirName"
                        val assetFiles = context.assets.list(assetPath) ?: emptyArray()
                        if (assetFiles.isNotEmpty()) {
                            assetFiles.forEach { fName ->
                                val entry = ZipEntry("$libDirName/$fName")
                                zipOut.putNextEntry(entry)
                                context.assets.open("$assetPath/$fName").use { it.copyTo(zipOut) }
                                zipOut.closeEntry()
                            }
                        } else {
                            // Write placeholder library.json if only manifest is present
                            val entry = ZipEntry("$libDirName/library.json")
                            zipOut.putNextEntry(entry)
                            val minimalLib = "{\"title\":\"$libDirName\",\"machineName\":\"${libDirName.substringBefore("-")}\",\"majorVersion\":1,\"minorVersion\":0,\"patchVersion\":0,\"runnable\":1}"
                            zipOut.write(minimalLib.toByteArray())
                            zipOut.closeEntry()
                        }
                    } catch (e: Exception) {
                        // ignore missing optional asset sub-files
                    }
                }
            }
        }

        return outFile
    }

    private fun parseLibraryJson(jsonStr: String, folderName: String): H5PLibraryEntity? {
        return try {
            val obj = JSONObject(jsonStr)
            val machineName = obj.optString("machineName", folderName.substringBefore("-"))
            val major = obj.optInt("majorVersion", 1)
            val minor = obj.optInt("minorVersion", 0)
            val patch = obj.optInt("patchVersion", 0)
            val title = obj.optString("title", machineName)
            val desc = obj.optString("description", "")
            val runnable = obj.optInt("runnable", 1) == 1
            val deps = obj.optJSONArray("preloadedDependencies")?.toString() ?: "[]"

            H5PLibraryEntity(
                id = "$machineName-$major.$minor",
                machineName = machineName,
                majorVersion = major,
                minorVersion = minor,
                patchVersion = patch,
                title = title,
                description = desc,
                runnable = runnable,
                isPrepackaged = false,
                sourcePath = "",
                dependenciesJson = deps
            )
        } catch (e: Exception) {
            null
        }
    }
}
