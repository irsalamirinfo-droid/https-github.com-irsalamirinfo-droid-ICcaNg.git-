package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.models.AutofillProfile
import com.example.data.models.GoogleForm
import com.example.data.models.GoogleFormWithProfile
import com.example.data.repository.FormRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class FormViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FormRepository

    val allProfiles: StateFlow<List<AutofillProfile>>
    val allFormsWithProfile: StateFlow<List<GoogleFormWithProfile>>

    // Search and Categorization logic
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Semua")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val filteredForms: StateFlow<List<GoogleFormWithProfile>>

    // WebView active form & active autofill profile session states
    private val _activeForm = MutableStateFlow<GoogleForm?>(null)
    val activeForm: StateFlow<GoogleForm?> = _activeForm.asStateFlow()

    private val _activeProfile = MutableStateFlow<AutofillProfile?>(null)
    val activeProfile: StateFlow<AutofillProfile?> = _activeProfile.asStateFlow()

    private val _webViewProgress = MutableStateFlow(0)
    val webViewProgress: StateFlow<Int> = _webViewProgress.asStateFlow()

    private val _isWebViewLoading = MutableStateFlow(false)
    val isWebViewLoading: StateFlow<Boolean> = _isWebViewLoading.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = FormRepository(database.formDao())

        allProfiles = repository.allProfiles
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        allFormsWithProfile = repository.allFormsWithProfile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        filteredForms = combine(
            allFormsWithProfile,
            _searchQuery,
            _selectedCategory
        ) { forms, query, category ->
            forms.filter { item ->
                val matchesQuery = item.form.title.contains(query, ignoreCase = true) ||
                        item.form.url.contains(query, ignoreCase = true) ||
                        item.form.category.contains(query, ignoreCase = true)
                val matchesCategory = category == "Semua" || item.form.category.equals(category, ignoreCase = true)
                matchesQuery && matchesCategory
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setWebViewProgress(progress: Int) {
        _webViewProgress.value = progress
        _isWebViewLoading.value = progress < 100
    }

    fun setWebViewLoading(isLoading: Boolean) {
        _isWebViewLoading.value = isLoading
        if (!isLoading) _webViewProgress.value = 100
    }

    fun openFormInApp(form: GoogleForm) {
        _activeForm.value = form
        viewModelScope.launch {
            // Fetch binding profile
            val profile = form.autoFillProfileId?.let { repository.getProfileById(it) }
            _activeProfile.value = profile
            
            // Mark last accessed in Local SQlite
            repository.updateLastAccessed(form.id, System.currentTimeMillis())
        }
    }

    fun setActiveProfile(profile: AutofillProfile?) {
        _activeProfile.value = profile
    }

    fun closeActiveForm() {
        _activeForm.value = null
        _activeProfile.value = null
        _webViewProgress.value = 0
        _isWebViewLoading.value = false
    }

    fun saveForm(form: GoogleForm) {
        viewModelScope.launch {
            repository.insertForm(form)
        }
    }

    fun deleteForm(form: GoogleForm) {
        viewModelScope.launch {
            repository.deleteForm(form)
        }
    }

    fun saveProfile(profile: AutofillProfile) {
        viewModelScope.launch {
            repository.insertProfile(profile)
        }
    }

    fun deleteProfile(profile: AutofillProfile) {
        viewModelScope.launch {
            repository.deleteProfile(profile)
        }
    }
}
