package com.example.flowlauncher.data.db

import androidx.room.*
import com.example.flowlauncher.data.model.AppInfo
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val appName: String,
    val lastUpdated: Long,
    val isSystemApp: Boolean,
    val iconPath: String
)

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE isSystemApp = 0 ORDER BY appName ASC")
    fun getNonSystemApps(): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Query("DELETE FROM apps")
    suspend fun deleteAllApps()

    @Query("SELECT * FROM apps WHERE appName LIKE '%' || :query || '%' OR packageName LIKE '%' || :query || '%'")
    fun searchApps(query: String): Flow<List<AppEntity>>
}

@Database(entities = [AppEntity::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 