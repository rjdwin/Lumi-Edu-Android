package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.H5PLibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Query("SELECT * FROM libraries ORDER BY machineName ASC, majorVersion DESC, minorVersion DESC")
    fun getAllLibraries(): Flow<List<H5PLibraryEntity>>

    @Query("SELECT * FROM libraries WHERE machineName = :machineName ORDER BY majorVersion DESC, minorVersion DESC LIMIT 1")
    suspend fun getLatestLibrary(machineName: String): H5PLibraryEntity?

    @Query("SELECT * FROM libraries WHERE machineName = :machineName AND majorVersion = :major AND minorVersion = :minor LIMIT 1")
    suspend fun getLibrary(machineName: String, major: Int, minor: Int): H5PLibraryEntity?

    @Query("SELECT COUNT(*) FROM libraries")
    suspend fun getLibraryCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibrary(library: H5PLibraryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraries(libraries: List<H5PLibraryEntity>)

    @Query("DELETE FROM libraries WHERE id = :id")
    suspend fun deleteLibrary(id: String)
}
