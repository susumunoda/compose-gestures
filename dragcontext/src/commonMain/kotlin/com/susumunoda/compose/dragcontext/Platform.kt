package com.susumunoda.compose.dragcontext

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform