package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.system.exitProcess

fun main() {
    val image = getImage("image")
    checkImage(image, "image")

    val watermark = getImage("watermark image")
    checkImage(watermark, "watermark")

    checkDimension(image, watermark)

    val outputImage = blendImages(image, watermark)

    val (outName, type, outputFile) = getOutputFileParams()
    ImageIO.write(outputImage, type, outputFile)
    println("The watermarked image $outName has been created.")
}

private fun getOutputFileParams(): Triple<String, String, File> {
    println("Input the output image filename (jpg or png extension):")
    val outName = readln()
    val type = outName.split(".").last()
    if (type != "jpg" && type != "png") {
        println("The output file extension isn't \"jpg\" or \"png\".")
        exitProcess(0)
    }
    val outputFile = File(outName)
    return Triple(outName, type, outputFile)
}

private fun getTransparency(): Int {
    val weight: Int
    println("Input the watermark transparency percentage (Integer 0-100):")
    try {
        weight = readln().toInt()
    } catch (e: Exception) {
        println("The transparency percentage isn't an integer number.")
        exitProcess(0)
    }
    if (weight !in 0..100) {
        println("The transparency percentage is out of range.")
        exitProcess(0)
    }
    return weight
}

private fun checkDimension(image: BufferedImage, watermark: BufferedImage) {
    if (image.width < watermark.width || image.height < watermark.height) {
        println("The watermark's dimensions are larger.")
        exitProcess(0)
    }
}

fun blendImages(image: BufferedImage, watermark: BufferedImage): BufferedImage {
    var transparencyColor: Color = Color(0, 255, 0)
    val outputImage = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
    var blend: (Color, Color, Int, Color) -> Color = { i, w, weight, tColor -> blendPixel(i, w, weight) }
    if (watermark.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        if (readln().lowercase() == "yes") blend = { i, w, weight, tColor -> blendPixelWithAlpha(i, w, weight) }
    } else {
        println("Do you want to set a transparency color?")
        if (readln().lowercase() == "yes") {
            transparencyColor = getTransparencyColor()
            blend = { i, w, weight, tColor -> blendPixelWithTColor(i, w, weight, transparencyColor) }
        }
    }
    val weight = getTransparency()

    var (xMin, xMax, yMin, yMax) = listOf(0, image.width - 1, 0, image.height - 1)
    println("Choose the position method (single, grid):")
    when (readln()) {
        "single" -> {
            println("Input the watermark position ([x 0-${image.width - watermark.width}] [y 0-${image.height - watermark.height}]):")
            try {
                val list = readln().split(" ").map { it.toInt() }
                xMin = list[0]
                yMin = list[1]
                xMax = xMin + watermark.width - 1
                yMax = yMin + watermark.height - 1
            } catch (e: Exception) {
                println("The position input is invalid.")
                exitProcess(0)
            }
            if (xMin < 0 || yMin < 0 || xMax > image.width - 1 || yMax > image.height - 1) {
                println("The position input is out of range.")
                exitProcess(0)
            }
        }
        "grid" -> {
            //do nothing
        }
        else -> {
            println("The position method input is invalid.")
            exitProcess(0)
        }
    }

    for (x in 0 until  image.width) {
        for (y in 0 until  image.height) {
            val i = Color(image.getRGB(x, y), true)
            if (x < xMin || x > xMax || y < yMin || y > yMax) {
                outputImage.setRGB(x, y, i.rgb)
            } else try {
                val w = Color(watermark.getRGB((x - xMin) % watermark.width, (y - yMin) % watermark.height), true)
                val color = blend(i, w, weight, transparencyColor)
                outputImage.setRGB(x, y, color.rgb)
            } catch (e: Exception) {
                exitProcess(0)
            }
        }
    }
    return outputImage
}

fun blendPixelWithTColor(i: Color, w: Color, weight: Int, transparencyColor: Color): Color {
    return if (w == transparencyColor) {
        i
    } else {
        Color(
            (weight * w.red + (100 - weight) * i.red) / 100,
            (weight * w.green + (100 - weight) * i.green) / 100,
            (weight * w.blue + (100 - weight) * i.blue) / 100,
            (weight * w.alpha + (100 - weight) * i.alpha) / 100
        )
    }
}

fun getTransparencyColor(): Color {
    val transparencyColor: Color
    println("Input a transparency color ([Red] [Green] [Blue]):")
    try {
        val list = readln().split(" ")
        if (list.size != 3) 5 / 0
        val (r, g, b) = list.map { it.toInt() }
        transparencyColor = Color(r, g, b)
    } catch (e: Exception) {
        println("The transparency color input is invalid.")
        exitProcess(0)
    }
    return transparencyColor
}

fun blendPixel(i: Color, w: Color, weight: Int): Color {
    return Color(
        (weight * w.red + (100 - weight) * i.red) / 100,
        (weight * w.green + (100 - weight) * i.green) / 100,
        (weight * w.blue + (100 - weight) * i.blue) / 100,
        255
    )
}

fun blendPixelWithAlpha(i: Color, w: Color, weight: Int): Color {
    return if (w.alpha == 0) {
        i
    } else {
        Color(
            (weight * w.red + (100 - weight) * i.red) / 100,
            (weight * w.green + (100 - weight) * i.green) / 100,
            (weight * w.blue + (100 - weight) * i.blue) / 100,
            (weight * w.alpha + (100 - weight) * i.alpha) / 100
        )
    }
}

fun checkImage(image: BufferedImage, s: String) {
    if (image.colorModel.numColorComponents != 3) {
        println("The number of $s color components isn't 3.")
        exitProcess(0)
    }
    if (image.colorModel.pixelSize != 24 && image.colorModel.pixelSize != 32) {
        println("The $s isn't 24 or 32-bit.")
        exitProcess(0)
    }
}

fun getImage(name: String): BufferedImage {
    println("Input the $name filename:")
    val fileName = readln()
    try {
        return ImageIO.read(File(fileName))
    } catch (e: Exception) {
        println("The file $fileName doesn't exist.")
        exitProcess(0)
    }
}

fun readImage(fileName: String): BufferedImage = ImageIO.read(File(fileName)).also {
    println("Image file: $fileName")
    println("Width: ${it.width}")
    println("Height: ${it.height}")
    println("Number of components: ${it.colorModel.numComponents}")
    println("Number of color components: ${it.colorModel.numColorComponents}")
    println("Bits per pixel: ${it.colorModel.pixelSize}")
    val transparency = when (it.transparency) {
        1 -> "OPAQUE"
        2 -> "BITMASK"
        else -> "TRANSLUCENT"
    }
    println("Transparency: $transparency")
}