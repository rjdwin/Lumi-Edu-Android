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

data class HubContentType(
    val id: String,
    val name: String,
    val summary: String,
    val description: String,
    val version: String,
    val isCore: Boolean,
    val category: String,
    val isInstalled: Boolean
)

data class DependencyStatus(
    val isSatisfied: Boolean,
    val missingLibraries: List<String>
)

class H5PLibraryManager(
    private val context: Context,
    private val libraryDao: LibraryDao
) {
    val allLibraries: Flow<List<H5PLibraryEntity>> = libraryDao.getAllLibraries()

    /**
     * Scans assets/h5p/libraries/ and ensures all pre-packaged libraries
     * are registered in the Room database.
     */
    suspend fun initializePrepackagedLibraries() = withContext(Dispatchers.IO) {
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

    /**
     * Reads the bundled H5P Hub Catalog cache (replicated from Lumi/H5P Hub API).
     */
    suspend fun getHubCatalog(): List<HubContentType> = withContext(Dispatchers.IO) {
        val list = mutableListOf<HubContentType>()
        try {
            val jsonStr = context.assets.open("h5p/hub-catalog.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonStr)
            val array = root.optJSONArray("contentTypes") ?: JSONArray()
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val id = item.optString("id")
                val name = item.optString("name")
                val summary = item.optString("summary")
                val desc = item.optString("description")
                val verObj = item.optJSONObject("version")
                val versionStr = if (verObj != null) "${verObj.optInt("major")}.${verObj.optInt("minor")}" else "1.0"
                val isCore = item.optBoolean("isCore", false)
                val category = item.optString("category", "General")

                val isInstalled = libraryDao.getLatestLibrary(id) != null

                list.add(
                    HubContentType(
                        id = id,
                        name = name,
                        summary = summary,
                        description = desc,
                        version = versionStr,
                        isCore = isCore,
                        category = category,
                        isInstalled = isInstalled
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
        }
        list
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
