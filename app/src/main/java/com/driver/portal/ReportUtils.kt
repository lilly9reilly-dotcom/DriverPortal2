package com.driver.portal

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ReportUtils {

    fun generateReportPdf(
        context: Context,
        driverName: String,
        carNumber: String,
        trips: String,
        loads: String,
        quantity: String,
        liters: String,
        profit: String,
        maintenance: String,
        net: String,
        distance: String
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val titlePaint = Paint().apply {
            textSize = 22f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            textSize = 16f
        }

        var y = 60

        canvas.drawText("Driver Report", 220f, y.toFloat(), titlePaint)
        y += 40

        canvas.drawText("Driver Name: $driverName", 40f, y.toFloat(), textPaint)
        y += 30
        canvas.drawText("Car Number: $carNumber", 40f, y.toFloat(), textPaint)
        y += 40

        canvas.drawText("Trips: $trips", 40f, y.toFloat(), textPaint)
        y += 25
        canvas.drawText("Loads: $loads", 40f, y.toFloat(), textPaint)
        y += 25
        canvas.drawText("Quantity: $quantity ton", 40f, y.toFloat(), textPaint)
        y += 25
        canvas.drawText("Liters: $liters L", 40f, y.toFloat(), textPaint)
        y += 25
        canvas.drawText("Profit: $profit IQD", 40f, y.toFloat(), textPaint)
        y += 25
        canvas.drawText("Maintenance: $maintenance IQD", 40f, y.toFloat(), textPaint)
        y += 25
        canvas.drawText("Net: $net IQD", 40f, y.toFloat(), textPaint)
        y += 25
        canvas.drawText("Distance: $distance KM", 40f, y.toFloat(), textPaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "driver_report.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "مشاركة التقرير"))
    }

    fun printPdf(context: Context, file: File) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager

        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes,
                cancellationSignal: android.os.CancellationSignal?,
                callback: LayoutResultCallback,
                extras: android.os.Bundle?
            ) {
                callback.onLayoutFinished(
                    PrintDocumentInfo.Builder("driver_report.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                        .build(),
                    true
                )
            }

            override fun onWrite(
                pages: Array<out android.print.PageRange>?,
                destination: ParcelFileDescriptor,
                cancellationSignal: android.os.CancellationSignal?,
                callback: WriteResultCallback
            ) {
                FileInputStream(file).use { input ->
                    FileOutputStream(destination.fileDescriptor).use { output ->
                        input.copyTo(output)
                    }
                }
                callback.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
            }
        }

        printManager.print("Driver Report", printAdapter, null)
    }
}