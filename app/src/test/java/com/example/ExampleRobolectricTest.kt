package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.LumiDatabase
import com.example.data.model.ActivityEntity
import com.example.data.model.ActivityType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lumi Mobile", appName)
    }

    @Test
    fun `database insert and query activity`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = LumiDatabase.getDatabase(context)
        val activity = ActivityEntity(
            id = "test_1",
            title = "Test Quiz",
            description = "A test activity",
            type = ActivityType.MULTI_CHOICE,
            mainLibrary = "H5P.MultiChoice",
            libraryVersion = "1.14",
            contentJson = "{}",
            h5pJson = "{}",
            isPreloaded = false,
            bestScore = 0,
            maxScore = 1
        )
        db.activityDao().insertActivity(activity)
        val loaded = db.activityDao().getActivityById("test_1")
        assertNotNull(loaded)
        assertEquals("Test Quiz", loaded?.title)
    }
}
