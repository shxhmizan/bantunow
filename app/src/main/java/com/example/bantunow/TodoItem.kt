package com.example.bantunow

import kotlinx.serialization.Serializable

@Serializable
data class TodoItem(
    val id: Int = 0,
    val name: String = ""
)
