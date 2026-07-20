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
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

    fun generateReportXlsx(
        context: Context,
        driverName: String,
        carNumber: String,
        periodLabel: String,
        summaryRows: List<Pair<String, String>>,
        tripHeaders: List<String>,
        tripRows: List<List<String>>
    ): File {
        val file = File(context.cacheDir, "driver_report.xlsx")
        ZipOutputStream(FileOutputStream(file)).use { zip ->
            writeZipEntry(zip, "[Content_Types].xml", buildContentTypesXml())
            writeZipEntry(zip, "_rels/.rels", buildRootRelsXml())
            writeZipEntry(zip, "xl/workbook.xml", buildWorkbookXml())
            writeZipEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRelsXml())
            writeZipEntry(zip, "xl/styles.xml", buildStylesXml())
            writeZipEntry(zip, "xl/worksheets/sheet1.xml", buildSummarySheetXml(driverName, carNumber, periodLabel, summaryRows))
            writeZipEntry(zip, "xl/worksheets/sheet2.xml", buildTripsSheetXml(tripHeaders, tripRows))
        }
        return file
    }

    fun shareXlsx(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "مشاركة ملف Excel"))
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

    private fun writeZipEntry(zip: ZipOutputStream, path: String, content: String) {
        zip.putNextEntry(ZipEntry(path))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
          <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
          <Default Extension="xml" ContentType="application/xml"/>
          <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
          <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
          <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
        </Types>
    """.trimIndent()

    private fun buildRootRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildWorkbookXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                  xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
          <sheets>
            <sheet name="Summary" sheetId="1" r:id="rId1"/>
            <sheet name="Trips" sheetId="2" r:id="rId2"/>
          </sheets>
        </workbook>
    """.trimIndent()

    private fun buildWorkbookRelsXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
          <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
          <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
          <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
        </Relationships>
    """.trimIndent()

    private fun buildStylesXml(): String = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="1"><font><sz val="11"/><name val="Calibri"/></font></fonts>
          <fills count="1"><fill><patternFill patternType="none"/></fill></fills>
          <borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
          <cellXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/></cellXfs>
          <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
    """.trimIndent()

    private fun buildSummarySheetXml(
        driverName: String,
        carNumber: String,
        periodLabel: String,
        summaryRows: List<Pair<String, String>>
    ): String {
        val rows = mutableListOf(
            listOf("تقرير السائق", driverName),
            listOf("رقم السيارة", carNumber),
            listOf("الفترة", periodLabel),
            listOf("", "")
        )
        summaryRows.forEach { (label, value) -> rows.add(listOf(label, value)) }
        return buildWorksheetXml(rows)
    }

    private fun buildTripsSheetXml(headers: List<String>, tripRows: List<List<String>>): String {
        val rows = mutableListOf(headers)
        rows.addAll(tripRows)
        return buildWorksheetXml(rows)
    }

    private fun buildWorksheetXml(rows: List<List<String>>): String {
        val body = rows.mapIndexed { rowIndex, row ->
            val cells = row.mapIndexed { cellIndex, value ->
                val ref = columnName(cellIndex + 1) + (rowIndex + 1)
                buildCell(ref, value)
            }.joinToString("")
            "<row r=\"${rowIndex + 1}\">$cells</row>"
        }.joinToString("")

        return """
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetData>$body</sheetData>
            </worksheet>
        """.trimIndent()
    }

    private fun buildCell(reference: String, value: String): String {
        val trimmed = value.trim()
        val numeric = trimmed.replace(",", "").toDoubleOrNull()
        return if (numeric != null && trimmed.matches(Regex("-?\\d+(?:\\.\\d+)?"))) {
            "<c r=\"$reference\"><v>${trimmed}</v></c>"
        } else {
            "<c r=\"$reference\" t=\"inlineStr\"><is><t>${escapeXml(value)}</t></is></c>"
        }
    }

    private fun columnName(index: Int): String {
        var n = index
        var result = ""
        while (n > 0) {
            val rem = (n - 1) % 26
            result = ('A' + rem) + result
            n = (n - 1) / 26
        }
        return result
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
    }
}