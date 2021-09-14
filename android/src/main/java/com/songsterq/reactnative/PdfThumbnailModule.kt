package com.songsterq.reactnative

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import com.facebook.react.bridge.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class PdfThumbnailModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

  override fun getName(): String {
    return "PdfThumbnail"
  }

  @ReactMethod
  fun generate(filePath: String, page: Int, promise: Promise) {
    var parcelFileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null
    try {
      parcelFileDescriptor = getParcelFileDescriptor(filePath)
      if (parcelFileDescriptor == null) {
        promise.reject("FILE_NOT_FOUND", "File $filePath not found")
        return
      }

      pdfRenderer = PdfRenderer(parcelFileDescriptor)
      if (page < 0 || page >= pdfRenderer.pageCount) {
        promise.reject("INVALID_PAGE", "Page number $page is invalid, file has ${pdfRenderer.pageCount} pages")
        return
      }

      val result = renderPage(pdfRenderer, page, filePath)
      promise.resolve(result)
    } catch (ex: IOException) {
      promise.reject("INTERNAL_ERROR", ex)
    } finally {
      pdfRenderer?.close()
      parcelFileDescriptor?.close()
    }
  }

  @ReactMethod
  fun generateWithMinSide(filePath: String, page: Int, minSide: Int, promise: Promise) {
    var parcelFileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null
    try {
      parcelFileDescriptor = getParcelFileDescriptor(filePath)
      if (parcelFileDescriptor == null) {
        promise.reject("FILE_NOT_FOUND", "File $filePath not found")
        return
      }

      pdfRenderer = PdfRenderer(parcelFileDescriptor)
      if (page < 0 || page >= pdfRenderer.pageCount) {
        promise.reject("INVALID_PAGE", "Page number $page is invalid, file has ${pdfRenderer.pageCount} pages")
        return
      }

      val result = renderPageWithMinSide(pdfRenderer, page, filePath, minSide)
      promise.resolve(result)
    } catch (ex: IOException) {
      promise.reject("INTERNAL_ERROR", ex)
    } finally {
      pdfRenderer?.close()
      parcelFileDescriptor?.close()
    }
  }

  @ReactMethod
  fun generateAllPages(filePath: String, promise: Promise) {
    var parcelFileDescriptor: ParcelFileDescriptor? = null
    var pdfRenderer: PdfRenderer? = null
    try {
      parcelFileDescriptor = getParcelFileDescriptor(filePath)
      if (parcelFileDescriptor == null) {
        promise.reject("FILE_NOT_FOUND", "File $filePath not found")
        return
      }

      pdfRenderer = PdfRenderer(parcelFileDescriptor)
      val result = WritableNativeArray()
      for (page in 0 until pdfRenderer.pageCount) {
        result.pushMap(renderPage(pdfRenderer, page, filePath))
      }
      promise.resolve(result)
    } catch (ex: IOException) {
      promise.reject("INTERNAL_ERROR", ex)
    } finally {
      pdfRenderer?.close()
      parcelFileDescriptor?.close()
    }
  }

  private fun getParcelFileDescriptor(filePath: String): ParcelFileDescriptor? {
    val uri = Uri.parse(filePath)
    if (ContentResolver.SCHEME_CONTENT == uri.scheme || ContentResolver.SCHEME_FILE == uri.scheme) {
      return this.reactApplicationContext.contentResolver.openFileDescriptor(uri, "r")
    } else if (filePath.startsWith("/")) {
      val file = File(filePath)
      return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }
    return null
  }

  private fun renderPage(pdfRenderer: PdfRenderer, page: Int, filePath: String): WritableNativeMap {
    return renderPageWithMinSide(pdfRenderer, page, filePath, null)
  }

  private fun renderPageWithMinSide(pdfRenderer: PdfRenderer, page: Int, filePath: String, minSide: Int?): WritableNativeMap {
    val currentPage = pdfRenderer.openPage(page)
    val originalWidth = currentPage.width
    val originalHeight = currentPage.height

    var factor = 1.0
    if (minSide !== null) {
      val originalMinSide = if(originalWidth < originalHeight) originalWidth else originalHeight
      if(originalMinSide < minSide) {
        factor = minSide / originalMinSide.toDouble()
      }
    }
    val width = (originalWidth * factor).toInt()
    val height = (originalHeight * factor).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
    currentPage.close()

    // Some bitmaps have transparent background which results in a black thumbnail. Add a white background.
    val bitmapWhiteBG = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
    bitmapWhiteBG.eraseColor(Color.WHITE)
    val canvas = Canvas(bitmapWhiteBG)
    canvas.drawBitmap(bitmap, 0f, 0f, null)
    bitmap.recycle()

    val outputFile = File.createTempFile(getOutputFilePrefix(filePath, page), ".jpg", reactApplicationContext.cacheDir)
    if (outputFile.exists()) {
      outputFile.delete()
    }
    val out = FileOutputStream(outputFile)
    bitmapWhiteBG.compress(Bitmap.CompressFormat.JPEG, 80, out)
    bitmapWhiteBG.recycle()
    out.flush()
    out.close()

    val map = WritableNativeMap()
    map.putString("uri", Uri.fromFile(outputFile).toString())
    map.putInt("width", width)
    map.putInt("height", height)
    return map
  }

  private fun getOutputFilePrefix(filePath: String, page: Int): String {
    val tokens = filePath.split("/")
    val originalFilename = tokens[tokens.lastIndex]
    val prefix = originalFilename.replace(".", "-")
    val generator = Random()
    val random = generator.nextInt(Integer.MAX_VALUE)
    return "$prefix-thumbnail-$page-$random"
  }
}
