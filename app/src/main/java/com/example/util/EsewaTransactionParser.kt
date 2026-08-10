package com.example.util

import android.content.Context
import android.net.Uri
import com.example.data.TransactionType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.InflaterInputStream

data class ParsedTransaction(
    val amount: Double?,
    val type: TransactionType,
    val category: String,
    val merchant: String,
    val note: String,
    val account: String,
    val sourceApp: String,
    val transactionId: String,
    val status: String,
    val dateStr: String,
    val timeStr: String,
    val timestamp: Long,
    val rawText: String,
    val isImageOrPdf: Boolean = false,
    val fileUriStr: String? = null,
    val fileName: String? = null
)

object EsewaTransactionParser {

    /**
     * Reads text streams from a PDF file Uri using Android ContentResolver.
     * Searches PDF byte streams, decompressing zlib/FlateDecode streams using InflaterInputStream,
     * and extracts string literals: (Text) Tj / TJ.
     */
    fun extractTextFromPdfUri(context: Context, uri: Uri): String {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return ""
            val bytes = inputStream.use { it.readBytes() }
            val extractedTextBuilder = StringBuilder()

            // 1. Search for stream ... endstream blocks and inflate them
            var searchOffset = 0
            val streamKeyword = "stream".toByteArray(Charsets.US_ASCII)
            val endStreamKeyword = "endstream".toByteArray(Charsets.US_ASCII)

            while (searchOffset < bytes.size - 10) {
                val streamPos = findBytes(bytes, streamKeyword, searchOffset)
                if (streamPos == -1) break

                var startData = streamPos + streamKeyword.size
                if (startData < bytes.size && bytes[startData] == '\r'.code.toByte()) startData++
                if (startData < bytes.size && bytes[startData] == '\n'.code.toByte()) startData++

                val endPos = findBytes(bytes, endStreamKeyword, startData)
                if (endPos == -1) break

                val streamData = bytes.copyOfRange(startData, endPos)
                searchOffset = endPos + endStreamKeyword.size

                // Decompress FlateDecode stream if possible
                val decompressed = tryDecompressFlate(streamData)
                val textContent = if (decompressed != null && decompressed.isNotEmpty()) {
                    String(decompressed, Charsets.UTF_8)
                } else {
                    String(streamData, Charsets.ISO_8859_1)
                }

                // Extract strings in parentheses (PDF string syntax: (String) Tj)
                val pdfLiteralRegex = Regex("""\(([^()]{1,400})\)""")
                pdfLiteralRegex.findAll(textContent).forEach { match ->
                    val textFragment = match.groupValues[1]
                        .replace("\\n", " ")
                        .replace("\\r", " ")
                        .replace("\\t", " ")
                        .replace("\\(", "(")
                        .replace("\\)", ")")
                        .trim()

                    // Filter out binary noise or empty lines
                    if (textFragment.length >= 1 && textFragment.any { it.isLetterOrDigit() }) {
                        extractedTextBuilder.append(textFragment).append("\n")
                    }
                }
            }

            val extractedResult = extractedTextBuilder.toString().trim()
            if (extractedResult.length > 20) {
                extractedResult
            } else {
                // Fallback: Scan uncompressed literals across raw byte string
                val rawString = String(bytes, Charsets.ISO_8859_1)
                val fallbackBuilder = StringBuilder()
                val pdfLiteralRegex = Regex("""\(([^()]{2,300})\)""")
                pdfLiteralRegex.findAll(rawString).forEach { match ->
                    val textFragment = match.groupValues[1]
                        .replace("\\n", " ")
                        .replace("\\r", " ")
                        .replace("\\(", "(")
                        .replace("\\)", ")")
                        .trim()

                    if (textFragment.length > 1 && textFragment.all { it.code in 32..126 }) {
                        fallbackBuilder.append(textFragment).append(" ")
                    }
                }
                fallbackBuilder.toString().trim()
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun findBytes(data: ByteArray, target: ByteArray, startFrom: Int): Int {
        if (startFrom >= data.size || target.isEmpty()) return -1
        val max = data.size - target.size
        for (i in startFrom..max) {
            var match = true
            for (j in target.indices) {
                if (data[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) return i
        }
        return -1
    }

    private fun tryDecompressFlate(compressed: ByteArray): ByteArray? {
        return try {
            val bais = ByteArrayInputStream(compressed)
            val iis = InflaterInputStream(bais)
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(2048)
            var len: Int
            while (iis.read(buffer).also { len = it } > 0) {
                baos.write(buffer, 0, len)
            }
            baos.toByteArray()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Parses transaction data from text, image URIs, or PDF file URIs.
     */
    fun parseWithContext(context: Context?, text: String?, fileUriStr: String? = null): ParsedTransaction {
        var combinedText = text?.trim() ?: ""
        var extractedFileName: String? = null

        if (!fileUriStr.isNullOrBlank() && context != null) {
            try {
                val uri = Uri.parse(fileUriStr)
                extractedFileName = uri.lastPathSegment ?: "PDF_Receipt.pdf"

                if (fileUriStr.contains(".pdf", ignoreCase = true) ||
                    context.contentResolver.getType(uri)?.contains("pdf", ignoreCase = true) == true
                ) {
                    val pdfText = extractTextFromPdfUri(context, uri)
                    if (pdfText.isNotBlank()) {
                        combinedText = if (combinedText.isNotBlank()) "$combinedText\n$pdfText" else pdfText
                    }
                }
            } catch (_: Exception) {
            }
        }

        val result = parse(combinedText, fileUriStr)
        return result.copy(fileName = extractedFileName)
    }

    fun parse(text: String?, fileUriStr: String? = null): ParsedTransaction {
        val raw = text?.trim() ?: ""
        val lower = raw.lowercase()
        val isFile = !fileUriStr.isNullOrBlank()

        // 1. Source App Recognition
        val sourceApp = when {
            lower.contains("esewa") -> "eSewa"
            lower.contains("fonepay") || lower.contains("phonepe") -> "FonePay"
            lower.contains("khalti") -> "Khalti"
            lower.contains("ime pay") || lower.contains("imepay") -> "IME Pay"
            lower.contains("connectips") || lower.contains("ips") -> "ConnectIPS"
            lower.contains("nabil") || lower.contains("nic asia") || lower.contains("global ime") ||
                    lower.contains("sanima") || lower.contains("prabhu") || lower.contains("bank") ||
                    lower.contains("acct") || lower.contains("a/c") -> "Mobile Banking"
            lower.contains("qr") -> "QR Payment"
            isFile && (fileUriStr?.endsWith(".pdf", ignoreCase = true) == true) -> "PDF Statement"
            isFile -> "Receipt Image"
            else -> "Share Import"
        }

        // 2. Merchant / Receiver Name Detection
        var merchant = ""
        val merchantPatterns = listOf(
            Regex("""(?i)(?:Merchant Name|Paid To|Receiver Name|Counter Name)[:\s\n\r]*([A-Za-z0-9\s&.\-_']{2,50})"""),
            Regex("""(?i)(?:Paid to|To|Merchant|Receiver)[:\s]*([A-Za-z0-9\s&.\-_']{2,50})"""),
            Regex("""(?i)(?:From|Sender|By)[:\s]*([A-Za-z0-9\s&.\-_']{2,50})""")
        )

        for (pattern in merchantPatterns) {
            val match = pattern.find(raw)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                    .replace(Regex("""(?i)^(?:is|was|code|currency|NPR|NPT|App|COMPLETE|SUCCESS|eSewa|Fonepay|Wallet)[:\s]*"""), "")
                    .trim()
                // Avoid picking up labels or currencies as merchant
                if (candidate.isNotBlank() &&
                    !candidate.equals("NPR", ignoreCase = true) &&
                    !candidate.equals("App", ignoreCase = true) &&
                    !candidate.equals("COMPLETE", ignoreCase = true) &&
                    !candidate.equals("eSewa Wallet", ignoreCase = true)
                ) {
                    merchant = candidate
                    break
                }
            }
        }

        // Line-by-line fallback for Merchant Name (eSewa / Fonepay format: Merchant Name \n : \n PRISA GIFT CENTER)
        if (merchant.isBlank()) {
            val lines = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            for (i in lines.indices) {
                val l = lines[i]
                if (l.contains("Merchant Name", ignoreCase = true) || l.contains("Paid To", ignoreCase = true)) {
                    val nextLine = lines.getOrNull(i + 1) ?: ""
                    val lineAfter = lines.getOrNull(i + 2) ?: ""
                    val value = if (nextLine != ":" && nextLine.isNotBlank()) nextLine else lineAfter
                    if (value.isNotBlank() && value != ":" && value.length > 2) {
                        merchant = value.trim()
                        break
                    }
                }
            }
        }

        // 3. Parse Amount
        var amountVal: Double? = null

        // Priority 1: Specific PDF fields "Amount : 279.00" or "Amount In Local Currency : 279"
        val explicitAmountPatterns = listOf(
            Regex("""(?i)(?:Amount|Amt|Total|Paid|Amount In Local Currency)[:\s\n\r]*(?:Rs\.?|NPR|NPT|रू|रू\.)?[\s]*([0-9,]+(?:\.[0-9]{1,2})?)"""),
            Regex("""(?i)(?:Rs\.?|NPR|INR|\$|रू|रू\.)[\s]*([0-9,]+(?:\.[0-9]{1,2})?)"""),
            Regex("""(?i)(?:Credited|Debited|Transferred)[:\s]*([0-9,]+(?:\.[0-9]{1,2})?)""")
        )

        for (regex in explicitAmountPatterns) {
            val match = regex.find(raw)
            if (match != null) {
                val cleaned = match.groupValues[1].replace(",", "")
                val parsed = cleaned.toDoubleOrNull()
                if (parsed != null && parsed > 0 && parsed < 10000000) {
                    amountVal = parsed
                    break
                }
            }
        }

        // Fallback line search for Amount (Amount \n : \n 279.00)
        if (amountVal == null) {
            val lines = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            for (i in lines.indices) {
                val l = lines[i]
                if (l.equals("Amount", ignoreCase = true) || l.startsWith("Amount", ignoreCase = true)) {
                    val nextLine = lines.getOrNull(i + 1) ?: ""
                    val lineAfter = lines.getOrNull(i + 2) ?: ""
                    val candidate = if (nextLine != ":" && nextLine.isNotBlank()) nextLine else lineAfter
                    val parsed = candidate.replace(",", "").toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        amountVal = parsed
                        break
                    }
                }
            }
        }

        // Broad regex fallback
        if (amountVal == null) {
            val fallbackMatch = Regex("""\b([0-9,]+\.[0-9]{2})\b""").find(raw)
            if (fallbackMatch != null) {
                amountVal = fallbackMatch.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }

        // 4. Transaction Type
        val isIncome = lower.contains("received") || lower.contains("credited") ||
                lower.contains("cashback") || lower.contains("refund") ||
                lower.contains("deposit") || lower.contains("income") ||
                lower.contains("added")
        val isSavings = lower.contains("saved") || lower.contains("fixed deposit") || lower.contains("fd")

        val type = when {
            isIncome -> TransactionType.INCOME
            isSavings -> TransactionType.SAVINGS
            else -> TransactionType.EXPENSE
        }

        // 5. Transaction ID / Ref Code
        var txnId = ""
        val txnPatterns = listOf(
            Regex("""(?i)(?:Reference Code|Ref Code|Txn ID|Transaction ID|Ref|Ref ID|Unique Request Id|Request Unique Id)[:\s#\n\r]*([A-Za-z0-9\-_]{5,35})"""),
            Regex("""\b([0-9A-Z]{6,20})\b""")
        )

        for (regex in txnPatterns) {
            val match = regex.find(raw)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length >= 5 && candidate != "COMPLETE" && candidate != "SUCCESS") {
                    txnId = candidate
                    break
                }
            }
        }

        if (txnId.isBlank()) {
            val lines = raw.split("\n").map { it.trim() }.filter { it.isNotBlank() }
            for (i in lines.indices) {
                val l = lines[i]
                if (l.contains("Reference Code", ignoreCase = true) || l.contains("Ref Code", ignoreCase = true)) {
                    val nextLine = lines.getOrNull(i + 1) ?: ""
                    val lineAfter = lines.getOrNull(i + 2) ?: ""
                    val value = if (nextLine != ":" && nextLine.isNotBlank()) nextLine else lineAfter
                    if (value.isNotBlank() && value != ":") {
                        txnId = value.trim()
                        break
                    }
                }
            }
        }

        if (txnId.isBlank()) {
            txnId = "TXN-${System.currentTimeMillis().toString().takeLast(8)}"
        }

        // 6. Date & Time
        val now = Date()
        val defaultDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val defaultTimeFormat = SimpleDateFormat("hh:mm a", Locale.US)

        var dateStr = defaultDateFormat.format(now)
        var timeStr = defaultTimeFormat.format(now)

        val dateMatch = Regex("""\b(\d{4}[-/]\d{1,2}[-/]\d{1,2}|\d{1,2}[-/]\d{1,2}[-/]\d{2,4})\b""").find(raw)
        if (dateMatch != null) {
            dateStr = dateMatch.groupValues[1]
        }

        val timeMatch = Regex("""\b(\d{1,2}:\d{2}(?::\d{2})?\s*(?:AM|PM|am|pm)?)\b""").find(raw)
        if (timeMatch != null) {
            timeStr = timeMatch.groupValues[1]
        }

        // 7. Status
        val status = when {
            lower.contains("complete") || lower.contains("success") -> "SUCCESS"
            lower.contains("failed") || lower.contains("rejected") -> "FAILED"
            lower.contains("pending") || lower.contains("processing") -> "PENDING"
            else -> "SUCCESS"
        }

        // 8. Auto Category Mapping Rules
        val category = when {
            // Purpose of payment explicit check
            lower.contains("lifestyle") || lower.contains("gift") || lower.contains("entertainment") -> "Shopping"

            // Restaurant -> Food
            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("bakery") ||
                    lower.contains("food") || lower.contains("hotel") || lower.contains("burger") ||
                    lower.contains("pizza") || lower.contains("momo") || lower.contains("kitchen") -> "Food & Dining"

            // Fuel Station -> Fuel / Transport
            lower.contains("fuel") || lower.contains("petrol") || lower.contains("diesel") ||
                    lower.contains("gas") || lower.contains("oil") || lower.contains("ride") ||
                    lower.contains("pathao") || lower.contains("indrive") || lower.contains("taxi") -> "Fuel & Transport"

            // Hospital -> Health
            lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("clinic") ||
                    lower.contains("doctor") || lower.contains("health") || lower.contains("medicine") -> "Health"

            // College / School -> Education
            lower.contains("college") || lower.contains("school") || lower.contains("university") ||
                    lower.contains("tuition") || lower.contains("fee") || lower.contains("education") ||
                    lower.contains("academy") -> "Education"

            // Electricity / Utilities -> Bills
            lower.contains("electricity") || lower.contains("water") || lower.contains("nea") ||
                    lower.contains("khanepani") || lower.contains("internet") || lower.contains("worldlink") ||
                    lower.contains("vianet") || lower.contains("subisu") || lower.contains("topup") ||
                    lower.contains("recharge") || lower.contains("ncell") || lower.contains("ntc") -> "Bills & Utilities"

            // Shopping
            lower.contains("mart") || lower.contains("bhatbhateni") || lower.contains("supermarket") ||
                    lower.contains("grocery") || lower.contains("groceries") || lower.contains("shopping") ||
                    lower.contains("store") || lower.contains("cloth") -> "Shopping"

            // Income / Salary
            lower.contains("salary") || lower.contains("wage") || lower.contains("bonus") -> "Salary"

            else -> if (type == TransactionType.INCOME) "Income" else "General"
        }

        // 9. Account / Payment Method
        var account = when {
            lower.contains("esewa wallet") || lower.contains("esewa") -> "eSewa Wallet"
            lower.contains("khalti") -> "Khalti Wallet"
            lower.contains("ime pay") || lower.contains("imepay") -> "IME Pay Wallet"
            lower.contains("fonepay") -> "FonePay Wallet"
            lower.contains("connectips") -> "ConnectIPS"
            lower.contains("mobile banking") || lower.contains("bank") -> "Bank Account"
            else -> "Digital Wallet"
        }

        // Extract Purpose of Payment for notes
        var note = raw
        val purposeMatch = Regex("""(?i)(?:Purpose Of Payment|Purpose|Description|Remarks)[:\s\n\r]*([^\n\r,.]+)""").find(raw)
        if (purposeMatch != null) {
            val rem = purposeMatch.groupValues[1].trim()
            if (rem.isNotBlank()) note = rem
        }

        return ParsedTransaction(
            amount = amountVal,
            type = type,
            category = category,
            merchant = merchant.ifBlank { if (sourceApp != "Share Import") sourceApp else "Merchant" },
            note = note.take(120),
            account = account,
            sourceApp = sourceApp,
            transactionId = txnId,
            status = status,
            dateStr = dateStr,
            timeStr = timeStr,
            timestamp = System.currentTimeMillis(),
            rawText = raw,
            isImageOrPdf = isFile,
            fileUriStr = fileUriStr
        )
    }

    fun parseUri(context: Context, uri: Uri): ParsedTransaction {
        val path = uri.toString()
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val fileName = uri.lastPathSegment ?: "Shared_Receipt"

        val pdfText = if (mimeType.contains("pdf", ignoreCase = true) || path.contains(".pdf", ignoreCase = true)) {
            extractTextFromPdfUri(context, uri)
        } else ""

        val textToParse = if (pdfText.isNotBlank()) pdfText else "Imported File Receipt: $fileName ($mimeType)"
        return parse(textToParse, fileUriStr = path).copy(fileName = fileName)
    }
}
