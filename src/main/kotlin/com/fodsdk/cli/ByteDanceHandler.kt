package com.fodsdk.cli

import com.fodsdk.utils.FileUtil
import java.io.File

object ByteDanceHandler {

    fun handleAndroidManifest(decompileDir: String) {
        val manifestFile = File(decompileDir, "AndroidManifest.xml")
        var manifest = manifestFile.readText()
        if (manifest.contains("android.permission.ACCESS_NETWORK_STATE").not()) {
            manifest = manifest.replace(
                "</application>",
                """
                </application>
                <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
            """.trimIndent()
            )
        }
        if (manifest.contains("android.permission.CHANGE_NETWORK_STATE").not()) {
            manifest = manifest.replace(
                "</application>",
                """
                </application>
                <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
            """.trimIndent()
            )
        }
        manifest = manifest.replace(
            "</application>",
            """
                </application>
               <queries>
                <package android:name="com.ss.android.ugc.aweme" />
                <package android:name="com.ss.android.ugc.aweme.lite" />
                <package android:name="com.ss.android.article.news" />
                <package android:name="com.ss.android.article.lite" />
                <package android:name="com.dragon.read" />
            </queries>
        """.trimIndent()
        )
        manifest = manifest.replace(
            "</application>",
            """
            <meta-data
                android:name="hume_convert.AppConvert.sdk.version"
                android:value="2.0.0" />
    
            <activity
                android:name="com.bytedance.ads.convert.BDBridgeActivity"
                android:exported="true"
                android:theme="@style/TranslucentTheme" />
        </application>
        """.trimIndent()
        )
        manifestFile.writeText(manifest)


        // TODO: style 要写入
        val valuesDir = File(decompileDir + File.separator + "res" + File.separator + "values")
        val stylesFile = File(valuesDir, "styles.xml")
        var styles = stylesFile.readText()
        styles = styles.replace(
            "</resources>",
            """
                     <style name="TranslucentTheme" parent="android:Theme.NoTitleBar">
                        <item name="android:windowBackground">@android:color/transparent</item>
                        <item name="android:colorBackgroundCacheHint">@null</item>
                        <item name="android:windowIsTranslucent">true</item>
                        <item name="android:windowAnimationStyle">@android:style/Animation</item>
                        <item name="android:windowNoTitle">true</item>
                        <item name="android:windowContentOverlay">@null</item>
                    </style>
                </resources>
            """.trimIndent()
        )
        stylesFile.writeText(styles)
    }

    fun patchChannelFile(decompileDir: String, patchFile: String) {
        FileUtil.patchPlugin(decompileDir, patchFile)
    }
}