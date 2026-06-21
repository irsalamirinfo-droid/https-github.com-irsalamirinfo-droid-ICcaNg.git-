package com.example.data.repository

import com.example.data.dao.FormDao
import com.example.data.models.AutofillProfile
import com.example.data.models.GoogleForm
import com.example.data.models.GoogleFormWithProfile
import kotlinx.coroutines.flow.Flow

class FormRepository(private val formDao: FormDao) {
    val allProfiles: Flow<List<AutofillProfile>> = formDao.getAllProfiles()
    val allFormsWithProfile: Flow<List<GoogleFormWithProfile>> = formDao.getAllFormsWithProfile()

    suspend fun getProfileById(id: Int): AutofillProfile? {
        return formDao.getProfileById(id)
    }

    suspend fun insertProfile(profile: AutofillProfile): Long {
        return formDao.insertProfile(profile)
    }

    suspend fun deleteProfile(profile: AutofillProfile) {
        formDao.deleteProfile(profile)
    }

    suspend fun getFormById(id: Int): GoogleForm? {
        return formDao.getFormById(id)
    }

    suspend fun insertForm(form: GoogleForm): Long {
        return formDao.insertForm(form)
    }

    suspend fun deleteForm(form: GoogleForm) {
        formDao.deleteForm(form)
    }

    suspend fun updateLastAccessed(formId: Int, timestamp: Long) {
        formDao.updateLastAccessed(formId, timestamp)
    }
}
