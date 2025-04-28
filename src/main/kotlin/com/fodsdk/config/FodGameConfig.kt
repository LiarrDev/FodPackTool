package com.fodsdk.config

import kotlinx.serialization.Serializable

@Serializable
data class FodGameConfig(
    var gid: String,
    var pid: String,
    var areaid: String,
    var osid: String,
    var client: String,
    var pkid: String,
    var pcid: String,
    var cid: String,
    var adid: String,
    var sdkver: String
)
