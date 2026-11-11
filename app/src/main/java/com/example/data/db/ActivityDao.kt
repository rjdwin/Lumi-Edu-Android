package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ActivityEntity
import com.example.data.model.AttemptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities ORDER BY updatedAt DESC")
    fun getAllActivities(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getActivityById(id: String): ActivityEntity?

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun getActivitiesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivities(activities: List<ActivityEntity>)

    @Update
    suspend fun updateActivity(activity: ActivityEntity)

    @Query("DELETE FROM activities WHERE id = :id")
    suspend fun deleteActivityById(id: String)

    @Query("UPDATE activities SET bestScore = CASE WHEN :score > bestScore THEN :score ELSE bestScore END, maxScore = :maxScore, attemptsCount = attemptsCount + 1, updatedAt = :timestamp WHERE id = :activityId")
    suspend fun recordAttempt(activityId: String, score: Int, maxScore: Int, timestamp: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: AttemptEntity)

    @Query("SELECT * FROM attempts WHERE activityId = :activityId ORDER BY timestamp DESC")
    fun getAttemptsForActivity(activityId: String): Flow<List<AttemptEntity>>
}
