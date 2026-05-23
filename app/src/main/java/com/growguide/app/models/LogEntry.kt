package com.growguide.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * A daily growth log entry for a specific plant.
 * Stored in Firestore at: users/{userId}/plants/{plantId}/logs/{logId}
 */
data class LogEntry(
    @get:PropertyName("entry")
    @set:PropertyName("entry")
    var entry: String = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    var id: String = ""
)
