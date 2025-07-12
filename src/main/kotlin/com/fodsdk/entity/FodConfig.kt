package com.fodsdk.entity

import kotlinx.serialization.Serializable

@Serializable
data class FodConfig(
    val gid: String = "",
    val pid: String = "",
    val areaid: String = "",
    val osid: String = "",
    val client: String = "",
    val pkid: String = "",
    val pcid: String = "",
    val cid: String = "",
    val ptid: String = "",
    val adid: String = "",
    var sdkver: String = ""
)