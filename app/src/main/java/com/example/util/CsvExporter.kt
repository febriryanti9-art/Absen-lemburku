package com.example.util

import com.example.data.AppLanguage
import com.example.data.OvertimeRecord
import com.example.data.OvertimeStatus
import com.example.data.UserProfile
import com.example.ui.i18n.stringsFor

object CsvExporter {

    fun buildCsvContent(
        entries: List<OvertimeRecord>,
        profile: UserProfile,
        language: AppLanguage
    ): String {
        val strings = stringsFor(language)
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel opens UTF-8 characters properly
        sb.append('\uFEFF')

        val isId = language == AppLanguage.INDONESIAN
        if (isId) {
            sb.appendLine("Tanggal,Karyawan,Mode,Kode Perusahaan,Jam Mulai,Jam Selesai,Lintas Hari,Durasi (Menit),Durasi,Status,Catatan")
        } else {
            sb.appendLine("Date,Employee,Mode,Company Code,Start Time,End Time,Overnight,Duration (Minutes),Duration,Status,Notes")
        }

        entries.forEach { entry ->
            val modeText = if (entry.personal) strings.modePersonalBadge else profile.companyName
            val statusText = when (entry.status) {
                OvertimeStatus.APPROVED -> if (entry.personal) strings.statusRecorded else strings.statusApproved
                OvertimeStatus.PENDING -> strings.statusPending
                OvertimeStatus.REJECTED -> strings.statusRejected
            }
            val overnightText = if (entry.overnight) {
                if (isId) "Ya" else "Yes"
            } else {
                if (isId) "Tidak" else "No"
            }
            val durationFormatted = DateTimeUtils.formatDurationFull(entry.durationMinutes, strings)
            val cols = listOf(
                entry.dateIso,
                entry.employeeName,
                modeText,
                entry.companyCode.ifBlank { "-" },
                entry.startTime,
                entry.endTime,
                overnightText,
                entry.durationMinutes.toString(),
                durationFormatted,
                statusText,
                entry.notes.ifBlank { "-" }
            )
            sb.appendLine(cols.joinToString(",") { escapeCsv(it) })
        }

        val totalMinutes = entries
            .filter { it.status != OvertimeStatus.REJECTED }
            .sumOf { it.durationMinutes }
        val totalDays = entries
            .filter { it.status != OvertimeStatus.REJECTED }
            .map { it.dateIso }
            .distinct()
            .size

        sb.appendLine()
        if (isId) {
            sb.appendLine("RINGKASAN TOTAL,,,,,,,$totalMinutes,${escapeCsv(DateTimeUtils.formatDurationFull(totalMinutes, strings))},$totalDays Hari,")
        } else {
            sb.appendLine("TOTAL SUMMARY,,,,,,,$totalMinutes,${escapeCsv(DateTimeUtils.formatDurationFull(totalMinutes, strings))},$totalDays Days,")
        }
        return sb.toString()
    }

    fun suggestedFileName(year: Int, month: Int): String {
        val ym = DateTimeUtils.yearMonthPrefix(year, month)
        return "Overtime_Report_$ym.csv"
    }

    private fun escapeCsv(value: String): String {
        val cleaned = value.replace("\r\n", " ").replace("\n", " ")
        return if (cleaned.contains(",") || cleaned.contains("\"")) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }
}
