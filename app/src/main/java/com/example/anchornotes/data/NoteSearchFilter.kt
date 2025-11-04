package com.example.anchornotes.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Filter model for searching notes.
 * Null values indicate "no filter" for that field.
 * Empty tagIds list means "no tag filter".
 */
@Parcelize
data class NoteSearchFilter(
    val query: String? = null,
    val tagIds: List<Long> = emptyList(),
    val fromDate: Long? = null,
    val toDate: Long? = null,
    val hasPhoto: Boolean? = null,
    val hasVoice: Boolean? = null,
    val hasLocation: Boolean? = null
) : Parcelable

