package com.fodsdk.utils

import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object DrawableUtil {

    fun replaceIcon(decompileDir: String, icon: File, sizeTag: String, iconName: String) {
        val mipmap = decompileDir + File.separator +
                "res" + File.separator +
                "mipmap-" + sizeTag + File.separator +
                iconName
        icon.replace(File(mipmap))

        val drawable = decompileDir + File.separator +
                "res" + File.separator +
                "drawable-" + sizeTag + File.separator +
                iconName
        icon.replace(File(drawable))

        val drawableV4 = decompileDir + File.separator +
                "res" + File.separator +
                "drawable-" + sizeTag +
                "-v4"
        if (File(drawableV4).exists()) {
            icon.replace(File(drawableV4 + File.separator + iconName))
        }
    }

    fun resizeImage(imgPath: String, width: Int, height: Int, newImgPath: String): File? {
        if (imgPath.isEmpty()) {
            return null
        }
        try {
            val src = ImageIO.read(File(imgPath))
            var imgColorMode = BufferedImage.TYPE_INT_RGB
            val suffix = imgPath.substring(imgPath.lastIndexOf(".") + 1)
            if ("png" == suffix) {
                imgColorMode = BufferedImage.TYPE_INT_ARGB
            }
            val image = BufferedImage(width, height, imgColorMode)
            image.graphics.drawImage(src, 0, 0, width, height, null)
            val file = File("$newImgPath\\app_icon.png")
            if (!file.exists()) {
                file.mkdirs()
            }
            ImageIO.write(image, suffix, file)
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun roundImage(input: File, output: File) {
        val originImg = ImageIO.read(input)

        val width = originImg.width
        val height = originImg.height
        val circleImage = BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR)

        // 获取 Graphics2D 对象
        val g2d = circleImage.createGraphics()

        // 设置透明背景
//        g2d.fillRect(0, 0, width, height)

        // 创建一个圆形的剪切区域
        val clip = Ellipse2D.Float(0f, 0f, width.toFloat(), height.toFloat())
        g2d.clip = clip
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // 绘制原始图片到圆形剪切区域内
        g2d.drawImage(originImg, 0, 0, null)

        // 释放图形上下文
        g2d.dispose()

        // 保存裁剪后的图片
        ImageIO.write(circleImage, "PNG", output)
    }
}