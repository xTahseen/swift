package com.musheer360.swiftslate.model

/** One message of a remembered conversation (see [Command.keepHistory]). */
data class ChatTurn(val fromUser: Boolean, val text: String)
