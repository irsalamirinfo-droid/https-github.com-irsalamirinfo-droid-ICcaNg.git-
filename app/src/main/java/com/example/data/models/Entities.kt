package com.example.data.models

import androidx.room.*

@Entity(tableName = "autofill_profiles")
data class AutofillProfile(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val profileName: String,
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val identityNumber: String = "", // NIP, NIM, ID, KTP, etc.
    val customFieldsJson: String = "{}" // JSON string of custom questions & answers
)

@Entity(
    tableName = "google_forms",
    foreignKeys = [
        ForeignKey(
            entity = AutofillProfile::class,
            parentColumns = ["id"],
            childColumns = ["autoFillProfileId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["autoFillProfileId"])]
)
data class GoogleForm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val category: String = "Lain-lain",
    val autoFillProfileId: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastAccessedAt: Long = 0L
)

data class GoogleFormWithProfile(
    @Embedded val form: GoogleForm,
    @Relation(
        parentColumn = "autoFillProfileId",
        entityColumn = "id"
    )
    val profile: AutofillProfile?
)
