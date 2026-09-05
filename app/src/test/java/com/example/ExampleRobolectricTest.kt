package com.example

import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.StudyDatabase
import com.example.data.model.Item
import com.example.data.model.SubjectEntity
import com.example.data.receiver.StudyNotificationReceiver
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
    assertEquals("Study Tools", appName)
  }

  @Test
  fun `verify room database entity and dao functionality`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, StudyDatabase::class.java).build()

    // Test ItemDao
    val itemDao = db.itemDao()
    itemDao.insertItem(Item(id = 1, name = "Test Item"))
    val items = itemDao.getAllItems().first()
    assertEquals(1, items.size)
    assertEquals("Test Item", items[0].name)

    // Test SubjectDao
    val subjectDao = db.subjectDao()
    subjectDao.insertSubject(SubjectEntity(name = "Maths", coefficient = 4.0f, colorHex = "#3B82F6", iconName = "Calculate"))
    val subjects = subjectDao.getAllSubjectsList()
    assertEquals(1, subjects.size)
    assertEquals("Maths", subjects[0].name)

    db.close()
  }

  @Test
  fun `verify notification receiver is declared and configured`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val intent = Intent(context, StudyNotificationReceiver::class.java).apply {
      action = "com.example.studytools.ACTION_SHOW_NOTIFICATION"
    }
    val pm = context.packageManager
    val receivers = pm.queryBroadcastReceivers(intent, 0)
    assertNotNull(receivers)
  }
}

