package com.example.saferoute.ui.start
data class PermissionModel(
    val title: String,
    val description: String,
    val iconRes: Int,
    val bannerRes: Int,
    val manifestPermission: String,
    var isGranted: Boolean = false
)