package com.andrerinas.openheadunit.newui.radio

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.andrerinas.openheadunit.utils.AppLog

data class LocalTrack(val id: Long, val title: String, val artist: String, val uri: Uri, val durationMs: Long)

/**
 * Real local audio browsing (this device's own MediaStore), used for the Radio screen's
 * "Bluetooth" tab. A true A2DP-sink "play the phone's music" experience needs a privileged system
 * build (see PhoneRepository/telephony docs) — playing this device's own music library is the
 * honest, fully-functional substitute described there.
 */
object LocalMediaLibrary {

    fun query(context: Context): List<LocalTrack> {
        val tracks = mutableListOf<LocalTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.TITLE} ASC LIMIT 100",
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    tracks += LocalTrack(
                        id = id,
                        title = cursor.getString(titleIdx) ?: "Unknown title",
                        artist = cursor.getString(artistIdx) ?: "Unknown artist",
                        uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id),
                        durationMs = cursor.getLong(durationIdx),
                    )
                }
            }
        } catch (e: SecurityException) {
            AppLog.w("LocalMediaLibrary: permission denied: ${e.message}")
        }
        return tracks
    }
}
