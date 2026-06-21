package com.example.data.dao

import androidx.room.*
import com.example.data.models.AutofillProfile
import com.example.data.models.GoogleForm
import com.example.data.models.GoogleFormWithProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface FormDao {
    // Profiles
    @Query("SELECT * FROM autofill_profiles ORDER BY profileName ASC")
    fun getAllProfiles(): Flow<List<AutofillProfile>>

    @Query("SELECT * FROM autofill_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): AutofillProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: AutofillProfile): Long

    @Delete
    suspend fun deleteProfile(profile: AutofillProfile)

    // Google Forms
    @Query("SELECT * FROM google_forms ORDER BY createdAt DESC")
    fun getAllForms(): Flow<List<GoogleForm>>

    @Transaction
    @Query("SELECT * FROM google_forms ORDER BY createdAt DESC")
    fun getAllFormsWithProfile(): Flow<List<GoogleFormWithProfile>>

    @Query("SELECT * FROM google_forms WHERE id = :id")
    suspend fun getFormById(id: Int): GoogleForm?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertForm(form: GoogleForm): Long

    @Delete
    suspend fun deleteForm(form: GoogleForm)

    @Query("UPDATE google_forms SET lastAccessedAt = :timestamp WHERE id = :formId")
    suspend fun updateLastAccessed(formId: Int, timestamp: Long)
}
