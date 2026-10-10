package com.musheer360.swiftslate.model

import androidx.compose.runtime.Immutable

enum class CommandType {
    AI, TEXT_REPLACER
}

@Immutable
data class Command(
    val trigger: String,
    val prompt: String,
    val isBuiltIn: Boolean = false,
    val type: CommandType = CommandType.AI,
    /** AI commands only: remember earlier exchanges and send them with each request. */
    val keepHistory: Boolean = false
)
