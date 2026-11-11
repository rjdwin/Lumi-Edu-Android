package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "libraries")
data class H5PLibraryEntity(
    @PrimaryKey
    val id: String, // e.g. "H5P.MultiChoice-1.14"
    val machineName: String, // e.g. "H5P.MultiChoice"
    val majorVersion: Int,
    val minorVersion: Int,
    val patchVersion: Int,
    val title: String,
    val description: String = "",
    val runnable: Boolean = true,
    val isPrepackaged: Boolean = true,
    val sourcePath: String = "", // "assets" or absolute path in filesDir/h5p_libraries
    val dependenciesJson: String = "[]", // JSON array of {machineName, majorVersion, minorVersion}
    val installedAt: Long = System.currentTimeMillis()
)
