package com.fodsdk.entity

import kotlinx.serialization.Serializable

@Serializable
data class FodConfigWrapper(
    val packageName: String = "",
    val name: String = "",
    val adParam: FodConfig = FodConfig()
)
