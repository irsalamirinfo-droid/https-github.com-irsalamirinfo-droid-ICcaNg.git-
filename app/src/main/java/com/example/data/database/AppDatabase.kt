package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.FormDao
import com.example.data.models.AutofillProfile
import com.example.data.models.GoogleForm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [AutofillProfile::class, GoogleForm::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun formDao(): FormDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "formauto_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class AppDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    val dao = database.formDao()
                    
                    // Seeding an Indonesian Profile
                    val profileId = dao.insertProfile(
                        AutofillProfile(
                            profileName = "Profil Utama (Budi Santoso)",
                            fullName = "Budi Santoso",
                            email = "budisantoso@example.com",
                            phone = "081234567890",
                            identityNumber = "2026104859",
                            customFieldsJson = """{
                              "kelas": "Informatika-A",
                              "instansi": "Universitas Indonesia",
                              "jurusan": "Teknik Informatika",
                              "alamat": "Jl. Merdeka No. 10, Jakarta",
                              "alasan": "Untuk meningkatkan skill otomasi digital dan produktivitas harian"
                            }""".trimIndent()
                        )
                    )

                    // Seeding Indonesian Form Links (pre-filled link templates)
                    dao.insertForm(
                        GoogleForm(
                            title = "Formulir Pendaftaran Seminar AI & Cloud (Contoh)",
                            url = "https://docs.google.com/forms/d/e/1FAIpQLSciWlQYQ-R_T0H0W6FvH0BfexT5e032j5W2LwI0q8g7FfXjGg/viewform",
                            category = "Pendaftaran",
                            autoFillProfileId = profileId.toInt()
                        )
                    )

                    dao.insertForm(
                        GoogleForm(
                            title = "Absensi Harian & Kehadiran Mahasiswa/Karyawan (Contoh)",
                            url = "https://docs.google.com/forms/d/e/1FAIpQLScyv8_8F_P9F_Gk0g3sWl-DndMvY39xJ7DveT2vU-yK4V7yQA/viewform",
                            category = "Absensi",
                            autoFillProfileId = profileId.toInt()
                        )
                    )

                    dao.insertForm(
                        GoogleForm(
                            title = "Kuesioner Feedback Layanan & Evaluasi Sistem (Contoh)",
                            url = "https://docs.google.com/forms/d/e/1FAIpQLSdO7uUuC2Bw67dJp6363TkyuO7X7_kLh_uPj7yvN6vY2zW4bA/viewform",
                            category = "Survey",
                            autoFillProfileId = profileId.toInt()
                        )
                    )
                }
            }
        }
    }
}
