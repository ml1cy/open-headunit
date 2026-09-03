package com.andrerinas.openheadunit.newui.camera

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.andrerinas.openheadunit.utils.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CameraUiState(
    val available: Boolean = false,
    val usingExternal: Boolean = false,
    val statusNote: String = "Starting camera…",
    val recording: Boolean = false,
    val lastSavedClip: String? = null,
    val guidesOn: Boolean = true,
)

/**
 * Real reverse/dash camera backing via CameraX. Prefers an EXTERNAL lens
 * ([CameraSelector.LENS_FACING_EXTERNAL]) since that's how a USB UVC camera surfaces on the
 * (uncommon) Android boards whose camera HAL bridges it — per ANDROID_IMPLEMENTATION.md there is
 * no universal UVC path on stock Android, so this falls back to the back camera, then front, and
 * is honest in [CameraUiState.statusNote] about which one it found.
 *
 * Also runs continuous recording once bound (a real "dashcam loop"): [saveClip] finalizes the
 * current recording to the device's video collection and immediately starts the next segment, so
 * the screen keeps recording the whole time it's open and "Save" always produces one real,
 * playable clip rather than nothing.
 */
class CameraController(private val context: Context) {

    private val _state = MutableStateFlow(CameraUiState())
    val state: StateFlow<CameraUiState> = _state.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var recorder: Recorder? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null

    fun toggleGuides() {
        _state.value = _state.value.copy(guidesOn = !_state.value.guidesOn)
    }

    /** Binds Preview (+ VideoCapture if permissions allow) to [lifecycleOwner]; call once the PreviewView surface exists. */
    fun bind(lifecycleOwner: LifecycleOwner, preview: Preview) {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener(
            {
                try {
                    val provider = future.get()
                    cameraProvider = provider
                    val (selector, status) = pickSelector(provider)
                    _state.value = _state.value.copy(available = status.available, usingExternal = status.usingExternal, statusNote = status.note)
                    if (!status.available) return@addListener

                    provider.unbindAll()
                    val useCases = mutableListOf<androidx.camera.core.UseCase>(preview)

                    if (hasPermission(Manifest.permission.CAMERA)) {
                        val rec = Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.SD)).build()
                        recorder = rec
                        val vc = VideoCapture.withOutput(rec)
                        videoCapture = vc
                        useCases += vc
                    }

                    provider.bindToLifecycle(lifecycleOwner, selector, *useCases.toTypedArray())
                    if (videoCapture != null && hasPermission(Manifest.permission.CAMERA)) {
                        startSegment()
                    }
                } catch (e: Exception) {
                    AppLog.w("CameraController: bind failed: ${e.message}")
                    _state.value = _state.value.copy(available = false, statusNote = "Camera unavailable: ${e.message}")
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun unbind() {
        stopSegment()
        cameraProvider?.unbindAll()
        cameraProvider = null
    }

    /** Finalizes the current dashcam segment (producing a real saved clip) and starts the next one. */
    fun saveClip() {
        val hadRecording = activeRecording != null
        stopSegment()
        startSegment()
        if (!hadRecording) {
            _state.value = _state.value.copy(statusNote = "Recording not available (camera or audio permission missing)")
        }
    }

    private fun startSegment() {
        val vc = videoCapture ?: return
        if (!hasPermission(Manifest.permission.CAMERA)) return
        val name = "openheadunit_dashcam_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}"
        val pending = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/OpenHeadunit")
            }
            val output = MediaStoreOutputOptions.Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(values)
                .build()
            vc.output.prepareRecording(context, output)
        } else {
            val dir = File(context.getExternalFilesDir(null), "dashcam").apply { mkdirs() }
            val output = FileOutputOptions.Builder(File(dir, "$name.mp4")).build()
            vc.output.prepareRecording(context, output)
        }

        val withAudio = hasPermission(Manifest.permission.RECORD_AUDIO)
        val recording = if (withAudio) pending.withAudioEnabled() else pending
        activeRecording = recording.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> _state.value = _state.value.copy(recording = true)
                is VideoRecordEvent.Finalize -> {
                    _state.value = _state.value.copy(
                        recording = false,
                        lastSavedClip = if (!event.hasError()) name else _state.value.lastSavedClip,
                    )
                }
                else -> Unit
            }
        }
    }

    private fun stopSegment() {
        activeRecording?.stop()
        activeRecording = null
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private data class SelectorStatus(val available: Boolean, val usingExternal: Boolean, val note: String)

    private fun pickSelector(provider: ProcessCameraProvider): Pair<CameraSelector, SelectorStatus> {
        val external = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_EXTERNAL).build()
        if (safeHasCamera(provider, external)) {
            return external to SelectorStatus(true, true, "External (USB) camera")
        }
        if (safeHasCamera(provider, CameraSelector.DEFAULT_BACK_CAMERA)) {
            return CameraSelector.DEFAULT_BACK_CAMERA to SelectorStatus(true, false, "Back camera · no external/USB camera detected")
        }
        if (safeHasCamera(provider, CameraSelector.DEFAULT_FRONT_CAMERA)) {
            return CameraSelector.DEFAULT_FRONT_CAMERA to SelectorStatus(true, false, "Front camera · no rear or external camera detected")
        }
        return CameraSelector.DEFAULT_BACK_CAMERA to SelectorStatus(false, false, "No camera available on this device")
    }

    private fun safeHasCamera(provider: ProcessCameraProvider, selector: CameraSelector): Boolean =
        try {
            provider.hasCamera(selector)
        } catch (e: Exception) {
            false
        }
}
