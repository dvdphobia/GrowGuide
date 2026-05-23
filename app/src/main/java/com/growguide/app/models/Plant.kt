package com.growguide.app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Represents a plant the user is growing.
 * Stored in Firestore at: users/{userId}/plants/{plantId}
 */
data class Plant(
    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("type")
    @set:PropertyName("type")
    var type: String = "",

    @get:PropertyName("notes")
    @set:PropertyName("notes")
    var notes: String = "",

    @get:PropertyName("createdAt")
    @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,

    @get:PropertyName("lastWatered")
    @set:PropertyName("lastWatered")
    var lastWatered: Timestamp? = null,

    @get:PropertyName("wateringFrequency")
    @set:PropertyName("wateringFrequency")
    var wateringFrequency: Int = 0,

    @get:PropertyName("photoUrl")
    @set:PropertyName("photoUrl")
    var photoUrl: String = "",

    // Plant ID is the Firestore document ID, not stored as a field
    var id: String = ""
)
