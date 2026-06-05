package com.renskylab.videoconverter

import android.net.Uri
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VideoMetadata(
    val codec: String = "Unknown",
    val bitrate: String = "Unknown",
    val frameRate: String = "Unknown",
    val resolution: String = "Unknown",
    val audioCodec: String = "Unknown",
    val duration: String = "Unknown"
)

sealed class ConversionStatus {
    object Idle : ConversionStatus()
    object Converting : ConversionStatus()
    data class Success(val outputPath: String) : ConversionStatus()
    data class Error(val message: String) : ConversionStatus()
}

enum class QualityPreset(val bitrate: String, val label: String) {
    LOW("1M", "Low (Small size)"),
    MEDIUM("5M", "Medium (Balanced)"),
    HIGH("12M", "High (Best quality)")
}

enum class ResolutionPreset(val scale: String?, val label: String) {
    ORIGINAL(null, "Original"),
    P1080("scale=-2:1080", "1080p"),
    P720("scale=-2:720", "720p"),
    P480("scale=-2:480", "480p")
}

class ConverterViewModel : ViewModel() {

    private val _selectedVideoUri = MutableStateFlow<Uri?>(null)
    val selectedVideoUri: StateFlow<Uri?> = _selectedVideoUri.asStateFlow()

    private val _originalMetadata = MutableStateFlow<VideoMetadata?>(null)
    val originalMetadata: StateFlow<VideoMetadata?> = _originalMetadata.asStateFlow()

    private val _conversionStatus = MutableStateFlow<ConversionStatus>(ConversionStatus.Idle)
    val conversionStatus: StateFlow<ConversionStatus> = _conversionStatus.asStateFlow()

    private val _conversionProgress = MutableStateFlow(0f)
    val conversionProgress: StateFlow<Float> = _conversionProgress.asStateFlow()

    private val _qualityPreset = MutableStateFlow(QualityPreset.MEDIUM)
    val qualityPreset: StateFlow<QualityPreset> = _qualityPreset.asStateFlow()

    private val _resolutionPreset = MutableStateFlow(ResolutionPreset.ORIGINAL)
    val resolutionPreset: StateFlow<ResolutionPreset> = _resolutionPreset.asStateFlow()

    private val _isAdvancedMode = MutableStateFlow(false)
    val isAdvancedMode: StateFlow<Boolean> = _isAdvancedMode.asStateFlow()

    // Library State
    private val _importedVideos = MutableStateFlow<List<java.io.File>>(emptyList())
    val importedVideos: StateFlow<List<java.io.File>> = _importedVideos.asStateFlow()

    private val _convertedVideos = MutableStateFlow<List<java.io.File>>(emptyList())
    val convertedVideos: StateFlow<List<java.io.File>> = _convertedVideos.asStateFlow()

    fun refreshVideoLists(importedDir: java.io.File, convertedDir: java.io.File) {
        _importedVideos.value = importedDir.listFiles()?.filter { it.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()
        _convertedVideos.value = convertedDir.listFiles()?.filter { it.extension == "mp4" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    // Advanced Settings
    private val _videoCodec = MutableStateFlow("hevc_mediacodec")
    val videoCodec: StateFlow<String> = _videoCodec.asStateFlow()

    private val _customBitrate = MutableStateFlow("5M")
    val customBitrate: StateFlow<String> = _customBitrate.asStateFlow()

    private val _frameRate = MutableStateFlow("Original")
    val frameRate: StateFlow<String> = _frameRate.asStateFlow()

    private val _audioCodec = MutableStateFlow("copy")
    val audioCodec: StateFlow<String> = _audioCodec.asStateFlow()

    private val _is10Bit = MutableStateFlow(false)
    val is10Bit: StateFlow<Boolean> = _is10Bit.asStateFlow()

    // New Advanced Settings
    private val _crf = MutableStateFlow("23")
    val crf: StateFlow<String> = _crf.asStateFlow()

    private val _encoderPreset = MutableStateFlow("medium")
    val encoderPreset: StateFlow<String> = _encoderPreset.asStateFlow()

    private val _cropEnabled = MutableStateFlow(false)
    val cropEnabled: StateFlow<Boolean> = _cropEnabled.asStateFlow()

    private val _cropW = MutableStateFlow("")
    val cropW: StateFlow<String> = _cropW.asStateFlow()

    private val _cropH = MutableStateFlow("")
    val cropH: StateFlow<String> = _cropH.asStateFlow()

    private val _cropX = MutableStateFlow("")
    val cropX: StateFlow<String> = _cropX.asStateFlow()

    private val _cropY = MutableStateFlow("")
    val cropY: StateFlow<String> = _cropY.asStateFlow()

    fun setSelectedVideo(uri: Uri) {
        _selectedVideoUri.value = uri
        _conversionStatus.value = ConversionStatus.Idle
        _conversionProgress.value = 0f
        _originalMetadata.value = null
    }

    fun extractMetadata(context: android.content.Context, uri: Uri) {
        val path = if (uri.scheme == "file") uri.path!! else com.antonkarpenko.ffmpegkit.FFmpegKitConfig.getSafParameterForRead(context, uri)
        com.antonkarpenko.ffmpegkit.FFprobeKit.getMediaInformationAsync(path) { session ->
            val info = session.mediaInformation
            if (info != null) {
                val videoStream = info.streams.find { it.type == "video" }
                val audioStream = info.streams.find { it.type == "audio" }
                
                val metadata = VideoMetadata(
                    codec = videoStream?.codec ?: "Unknown",
                    bitrate = if (info.bitrate != null) "${info.bitrate.toLong() / 1000} kbps" else "Unknown",
                    frameRate = videoStream?.averageFrameRate ?: "Unknown",
                    resolution = if (videoStream != null) "${videoStream.width}x${videoStream.height}" else "Unknown",
                    audioCodec = audioStream?.codec ?: "Unknown",
                    duration = if (info.duration != null) "${info.duration.toDouble().toInt()}s" else "Unknown"
                )
                _originalMetadata.value = metadata
            }
        }
    }

    fun setQualityPreset(preset: QualityPreset) {
        _qualityPreset.value = preset
    }

    fun setResolutionPreset(preset: ResolutionPreset) {
        _resolutionPreset.value = preset
    }

    fun setAdvancedMode(enabled: Boolean) {
        _isAdvancedMode.value = enabled
    }

    fun setVideoCodec(codec: String) {
        _videoCodec.value = codec
    }

    fun setCustomBitrate(bitrate: String) {
        _customBitrate.value = bitrate
    }

    fun setFrameRate(fr: String) {
        _frameRate.value = fr
    }

    fun setAudioCodec(codec: String) {
        _audioCodec.value = codec
    }

    fun setIs10Bit(enabled: Boolean) {
        _is10Bit.value = enabled
    }

    fun setCrf(value: String) {
        _crf.value = value
    }

    fun setEncoderPreset(value: String) {
        _encoderPreset.value = value
    }

    fun setCropEnabled(enabled: Boolean) {
        _cropEnabled.value = enabled
    }

    fun setCropDimensions(w: String, h: String, x: String, y: String) {
        _cropW.value = w
        _cropH.value = h
        _cropX.value = x
        _cropY.value = y
    }

    fun setStatus(status: ConversionStatus) {
        _conversionStatus.value = status
    }

    fun setProgress(progress: Float) {
        _conversionProgress.value = progress
    }

    fun buildFFmpegCommand(inputPath: String, outputPath: String): String {
        val sb = StringBuilder("-hwaccel mediacodec ")
        
        // Input
        sb.append("-i \"$inputPath\" ")

        val videoFilters = mutableListOf<String>()
        
        // Ensure dimensions are even (required by many hardware encoders)
        val alignmentFilter = "scale=trunc(iw/2)*2:trunc(ih/2)*2"

        if (_isAdvancedMode.value) {
            // Advanced Video Settings
            sb.append("-c:v ${_videoCodec.value} ")
            
            val isHardware = _videoCodec.value.contains("mediacodec")
            
            if (isHardware) {
                sb.append("-b:v ${_customBitrate.value} ")
                if (_is10Bit.value && _videoCodec.value == "hevc_mediacodec") {
                    sb.append("-profile:v main10 ")
                }
            } else {
                sb.append("-crf ${_crf.value} ")
                sb.append("-preset ${_encoderPreset.value} ")
            }
            
            // Resolution Scaling
            val baseScale = _resolutionPreset.value.scale ?: alignmentFilter
            if (_resolutionPreset.value.scale != null) {
                // Combine alignment with resolution
                videoFilters.add("${_resolutionPreset.value.scale},scale=trunc(iw/2)*2:trunc(ih/2)*2")
            } else {
                videoFilters.add(alignmentFilter)
            }

            if (_cropEnabled.value) {
                val w = _cropW.value.ifEmpty { "in_w" }
                val h = _cropH.value.ifEmpty { "in_h" }
                val x = _cropX.value.ifEmpty { "0" }
                val y = _cropY.value.ifEmpty { "0" }
                videoFilters.add("crop=trunc($w/2)*2:trunc($h/2)*2:trunc($x/2)*2:trunc($y/2)*2")
            }

            if (_frameRate.value != "Original") {
                sb.append("-r ${_frameRate.value} ")
            }

            // HDR Metadata
            if (_is10Bit.value) {
                sb.append("-color_range tv -colorspace bt2020nc -color_trc smpte2084 -color_primaries bt2020 ")
            }

            // Audio
            sb.append("-c:a ${_audioCodec.value} ")
            if (_audioCodec.value == "aac") {
                sb.append("-b:a 128k ")
            }
            
            // Pixel Format: nv12 is more native for MediaCodec
            val pixFmt = if (_is10Bit.value) "p010le" else if (isHardware) "nv12" else "yuv420p"
            sb.append("-pix_fmt $pixFmt ")

        } else {
            // Simple Mode
            sb.append("-c:v hevc_mediacodec ")
            sb.append("-b:v ${_qualityPreset.value.bitrate} ")
            
            videoFilters.add(alignmentFilter)
            _resolutionPreset.value.scale?.let { videoFilters.add(it) }
            
            sb.append("-c:a copy ")
            sb.append("-pix_fmt nv12 ")
        }

        if (videoFilters.isNotEmpty()) {
            sb.append("-vf \"${videoFilters.joinToString(",")}\" ")
        }

        val tag = if (_videoCodec.value.contains("hevc") || _videoCodec.value.contains("265")) "hvc1" else "avc1"
        sb.append("-tag:v $tag -y \"$outputPath\"")
        
        val cmd = sb.toString()
        android.util.Log.d("FFmpegGPU", "Generated Command: $cmd")
        return cmd
    }
}
