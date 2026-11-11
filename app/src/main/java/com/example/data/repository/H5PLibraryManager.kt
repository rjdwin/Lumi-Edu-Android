package com.example.data.repository

import android.content.Context
import com.example.data.db.LibraryDao
import com.example.data.model.H5PLibraryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class H5PLibraryManager(
    private val context: Context,
    private val libraryDao: LibraryDao
) {
    val allLibraries: Flow<List<H5PLibraryEntity>> = libraryDao.getAllLibraries()

    suspend fun initializePrepackagedLibraries() = withContext(Dispatchers.IO) {
        val count = libraryDao.getLibraryCount()
        if (count == 0) {
            try {
                val libFolders = context.assets.list("h5p/libraries") ?: emptyArray()
                val entities = mutableListOf<H5PLibraryEntity>()
                for (folder in libFolders) {
                    try {
                        val jsonStr = context.assets.open("h5p/libraries/$folder/library.json").bufferedReader().use { it.readText() }
                        val obj = JSONObject(jsonStr)
                        val machineName = obj.optString("machineName")
                        val major = obj.optInt("majorVersion")
                        val minor = obj.optInt("minorVersion")
                        val patch = obj.optInt("patchVersion")
                        val title = obj.optString("title", machineName)
                        val desc = obj.optString("description", "")
                        val runnable = obj.optInt("runnable", 1) == 1
                        val deps = obj.optJSONArray("preloadedDependencies")?.toString() ?: "[]"

                        entities.add(
                            H5PLibraryEntity(
                                id = "$machineName-$major.$minor",
                                machineName = machineName,
                                majorVersion = major,
                                minorVersion = minor,
                                patchVersion = patch,
                                title = title,
                                description = desc,
                                runnable = runnable,
                                isPrepackaged = true,
                                sourcePath = "assets/h5p/libraries/$folder",
                                dependenciesJson = deps
                            )
                        )
                    } catch (e: Exception) {
                        // ignore malformed or missing library.json
                    }
                }
                if (entities.isNotEmpty()) {
                    libraryDao.insertLibraries(entities)
                }
            } catch (e: Exception) {
                // assets list error
            }
        }
    }

    suspend fun checkDependenciesForLibrary(machineName: String, major: Int, minor: Int): DependencyStatus = withContext(Dispatchers.IO) {
        val lib = libraryDao.getLibrary(machineName, major, minor)
            ?: libraryDao.getLatestLibrary(machineName)
            ?: return@withContext DependencyStatus(
                isSatisfied = false,
                missingLibraries = listOf("$machineName $major.$minor")
            )

        val depsArray = try { JSONArray(lib.dependenciesJson) } catch (e: Exception) { JSONArray() }
        val missing = mutableListOf<String>()

        for (i in 0 until depsArray.length()) {
            val dep = depsArray.getJSONObject(i)
            val depName = dep.optString("machineName")
            val depMaj = dep.optInt("majorVersion")
            val depMin = dep.optInt("minorVersion")

            val found = libraryDao.getLibrary(depName, depMaj, depMin) ?: libraryDao.getLatestLibrary(depName)
            if (found == null) {
                missing.add("$depName $depMaj.$depMin")
            }
        }

        DependencyStatus(
            isSatisfied = missing.isEmpty(),
            missingLibraries = missing
        )
    }

    suspend fun deleteLibrary(id: String) = withContext(Dispatchers.IO) {
        libraryDao.deleteLibrary(id)
        val dir = File(context.filesDir, "h5p_libraries/$id")
        if (dir.exists()) dir.deleteRecursively()
    }
}

data class DependencyStatus(
    val isSatisfied: Boolean,
    val missingLibraries: List<String>
)
