package com.renskylab.videoconverter

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.antonkarpenko.ffmpegkit.FFmpegKit
import com.antonkarpenko.ffmpegkit.FFmpegKitConfig
import com.antonkarpenko.ffmpegkit.ReturnCode
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.io.File

class ConverterFragment : Fragment() {

    private val viewModel: ConverterViewModel by activityViewModels()
    private var player: ExoPlayer? = null

    // UI Components
    private lateinit var playerView: PlayerView
    private lateinit var noVideoPlaceholder: LinearLayout
    private lateinit var selectVideoButton: MaterialButton
    private lateinit var convertButton: MaterialButton
    private lateinit var saveToGalleryButton: MaterialButton
    private lateinit var statusTextView: TextView
    private lateinit var logTextView: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var clearLogButton: MaterialButton
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var simpleSettingsLayout: LinearLayout
    private lateinit var advancedSettingsLayout: LinearLayout
    private lateinit var toggleGroup: MaterialButtonToggleGroup
    private lateinit var qualityChipGroup: ChipGroup
    private lateinit var resolutionDropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView
    private lateinit var resolutionInputLayout: TextInputLayout
    private lateinit var videoCodecDropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView
    private lateinit var videoCodecInputLayout: TextInputLayout
    private lateinit var frameRateDropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView
    private lateinit var frameRateInputLayout: TextInputLayout
    private lateinit var audioCodecDropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView
    private lateinit var audioCodecInputLayout: TextInputLayout
    private lateinit var bitrateEditText: EditText
    private lateinit var bitrateInputLayout: TextInputLayout
    private lateinit var softwareSettingsLayout: LinearLayout
    private lateinit var crfEditText: EditText
    private lateinit var crfInputLayout: TextInputLayout
    private lateinit var presetDropdown: com.google.android.material.textfield.MaterialAutoCompleteTextView
    private lateinit var presetInputLayout: TextInputLayout
    private lateinit var cropSwitch: MaterialSwitch
    private lateinit var cropInputsLayout: ViewGroup
    private lateinit var cropWEditText: EditText
    private lateinit var cropWInputLayout: TextInputLayout
    private lateinit var cropHEditText: EditText
    private lateinit var cropHInputLayout: TextInputLayout
    private lateinit var cropXEditText: EditText
    private lateinit var cropXInputLayout: TextInputLayout
    private lateinit var cropYEditText: EditText
    private lateinit var cropYInputLayout: TextInputLayout
    private lateinit var infoQuality: ImageView
    private lateinit var infoCrop: ImageView
    private lateinit var hdrSwitch: MaterialSwitch
    private lateinit var infoHdr: ImageView
    private lateinit var metadataCard: com.google.android.material.card.MaterialCardView
    private lateinit var originalCodecText: TextView
    private lateinit var originalResText: TextView
    private lateinit var originalBitrateText: TextView
    private lateinit var originalFpsText: TextView
    private lateinit var toggleLogsButton: MaterialButton
    private lateinit var logCard: com.google.android.material.card.MaterialCardView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_converter, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initUi(view)
        setupInfoTooltips()
        observeState()
    }

    private fun initUi(v: View) {
        playerView = v.findViewById(R.id.playerView)
        noVideoPlaceholder = v.findViewById(R.id.noVideoPlaceholder)
        selectVideoButton = v.findViewById(R.id.selectVideoButton)
        convertButton = v.findViewById(R.id.convertButton)
        saveToGalleryButton = v.findViewById(R.id.saveToGalleryButton)
        statusTextView = v.findViewById(R.id.statusTextView)
        logTextView = v.findViewById(R.id.logTextView)
        logScrollView = v.findViewById(R.id.logScrollView)
        clearLogButton = v.findViewById(R.id.clearLogButton)
        toggleLogsButton = v.findViewById(R.id.toggleLogsButton)
        logCard = v.findViewById(R.id.logCard)
        progressBar = v.findViewById(R.id.conversionProgressBar)

        clearLogButton.setOnClickListener { logTextView.text = "" }
        toggleLogsButton.setOnClickListener {
            val isVisible = logCard.visibility == View.VISIBLE
            logCard.visibility = if (isVisible) View.GONE else View.VISIBLE
            toggleLogsButton.text = if (isVisible) "Show Technical Logs" else "Hide Technical Logs"
        }
        simpleSettingsLayout = v.findViewById(R.id.simpleSettingsLayout)
        advancedSettingsLayout = v.findViewById(R.id.advancedSettingsLayout)
        toggleGroup = v.findViewById(R.id.toggleGroup)
        qualityChipGroup = v.findViewById(R.id.qualityChipGroup)
        resolutionDropdown = v.findViewById(R.id.resolutionDropdown)
        resolutionInputLayout = v.findViewById(R.id.resolutionInputLayout)
        videoCodecDropdown = v.findViewById(R.id.videoCodecDropdown)
        videoCodecInputLayout = v.findViewById(R.id.videoCodecInputLayout)
        frameRateDropdown = v.findViewById(R.id.frameRateDropdown)
        frameRateInputLayout = v.findViewById(R.id.frameRateInputLayout)
        audioCodecDropdown = v.findViewById(R.id.audioCodecDropdown)
        audioCodecInputLayout = v.findViewById(R.id.audioCodecInputLayout)
        bitrateEditText = v.findViewById(R.id.bitrateEditText)
        bitrateInputLayout = v.findViewById(R.id.bitrateInputLayout)
        softwareSettingsLayout = v.findViewById(R.id.softwareSettingsLayout)
        crfEditText = v.findViewById(R.id.crfEditText)
        crfInputLayout = v.findViewById(R.id.crfInputLayout)
        presetDropdown = v.findViewById(R.id.presetDropdown)
        presetInputLayout = v.findViewById(R.id.presetInputLayout)
        cropSwitch = v.findViewById(R.id.cropSwitch)
        cropInputsLayout = v.findViewById(R.id.cropInputsLayout)
        cropWEditText = v.findViewById(R.id.cropWEditText)
        cropWInputLayout = v.findViewById(R.id.cropWInputLayout)
        cropHEditText = v.findViewById(R.id.cropHEditText)
        cropHInputLayout = v.findViewById(R.id.cropHInputLayout)
        cropXEditText = v.findViewById(R.id.cropXEditText)
        cropXInputLayout = v.findViewById(R.id.cropXInputLayout)
        cropYEditText = v.findViewById(R.id.cropYEditText)
        cropYInputLayout = v.findViewById(R.id.cropYInputLayout)
        infoQuality = v.findViewById(R.id.infoQuality)
        infoCrop = v.findViewById(R.id.infoCrop)
        hdrSwitch = v.findViewById(R.id.hdrSwitch)
        infoHdr = v.findViewById(R.id.infoHdr)
        metadataCard = v.findViewById(R.id.metadataCard)
        originalCodecText = v.findViewById(R.id.originalCodecText)
        originalResText = v.findViewById(R.id.originalResText)
        originalBitrateText = v.findViewById(R.id.originalBitrateText)
        originalFpsText = v.findViewById(R.id.originalFpsText)

        hdrSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setIs10Bit(isChecked)
        }
        infoHdr.setOnClickListener {
            showInfoDialog("10-bit / HDR", "Enables High Dynamic Range support by using 10-bit depth (p010le). Recommended for modern Snapdragon devices and high-quality source files to prevent washed-out colors.")
        }

        selectVideoButton.setOnClickListener {
            findNavController().navigate(R.id.importedFragment)
        }
        convertButton.setOnClickListener { startConversion() }
        saveToGalleryButton.setOnClickListener {
            val status = viewModel.conversionStatus.value
            if (status is ConversionStatus.Success) {
                (activity as? MainActivity)?.exportVideoToGallery(File(status.outputPath))
            }
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) viewModel.setAdvancedMode(checkedId == R.id.btnAdvanced)
        }

        qualityChipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            val preset = when (checkedIds.firstOrNull()) {
                R.id.chipLow -> QualityPreset.LOW
                R.id.chipHigh -> QualityPreset.HIGH
                else -> QualityPreset.MEDIUM
            }
            viewModel.setQualityPreset(preset)
        }

        val resValues = ResolutionPreset.values()
        resolutionDropdown.setSimpleItems(resValues.map { it.label }.toTypedArray())
        resolutionDropdown.setText(viewModel.resolutionPreset.value.label, false)
        resolutionDropdown.setOnItemClickListener { _, _, position, _ -> 
            viewModel.setResolutionPreset(resValues[position])
        }

        val videoCodecs = listOf("hevc_mediacodec", "h264_mediacodec", "libx264", "libx265", "libvpx-vp9", "libaom-av1")
        videoCodecDropdown.setSimpleItems(videoCodecs.toTypedArray())
        videoCodecDropdown.setText(viewModel.videoCodec.value, false)
        videoCodecDropdown.setOnItemClickListener { _, _, position, _ ->
            val codec = videoCodecs[position]
            viewModel.setVideoCodec(codec)
            updateCodecSpecificUi(codec)
        }
        updateCodecSpecificUi(viewModel.videoCodec.value)

        val frameRates = listOf("Original", "60", "30", "24")
        frameRateDropdown.setSimpleItems(frameRates.toTypedArray())
        frameRateDropdown.setText(viewModel.frameRate.value, false)
        frameRateDropdown.setOnItemClickListener { _, _, position, _ -> 
            viewModel.setFrameRate(frameRates[position])
        }

        val audioCodecs = listOf("copy", "aac")
        audioCodecDropdown.setSimpleItems(audioCodecs.toTypedArray())
        audioCodecDropdown.setText(viewModel.audioCodec.value, false)
        audioCodecDropdown.setOnItemClickListener { _, _, position, _ -> 
            viewModel.setAudioCodec(audioCodecs[position])
        }

        bitrateEditText.setText(viewModel.customBitrate.value)
        bitrateEditText.addTextChangedListener(SimpleTextWatcher { viewModel.setCustomBitrate(it) })
        
        crfEditText.setText(viewModel.crf.value)
        crfEditText.addTextChangedListener(SimpleTextWatcher { viewModel.setCrf(it) })

        val presets = listOf("ultrafast", "superfast", "veryfast", "faster", "fast", "medium", "slow", "slower", "veryslow")
        presetDropdown.setSimpleItems(presets.toTypedArray())
        presetDropdown.setText(viewModel.encoderPreset.value, false)
        presetDropdown.setOnItemClickListener { _, _, position, _ -> 
            viewModel.setEncoderPreset(presets[position])
        }

        cropSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setCropEnabled(isChecked)
            cropInputsLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val cropWatcher = SimpleTextWatcher {
            viewModel.setCropDimensions(cropWEditText.text.toString(), cropHEditText.text.toString(),
                cropXEditText.text.toString(), cropYEditText.text.toString())
        }
        cropWEditText.addTextChangedListener(cropWatcher)
        cropHEditText.addTextChangedListener(cropWatcher)
        cropXEditText.addTextChangedListener(cropWatcher)
        cropYEditText.addTextChangedListener(cropWatcher)
    }

    private fun setupInfoTooltips() {
        infoQuality.setOnClickListener { showInfoDialog("Quality Preset", "A quick way to balance file size and video quality.") }
        infoCrop.setOnClickListener { showInfoDialog("Video Cropping", "Trims the edges of the video.") }
        infoHdr.setOnClickListener { showInfoDialog("10-bit / HDR", "Enables High Dynamic Range support by using 10-bit depth.") }

        videoCodecInputLayout.setStartIconOnClickListener { showInfoDialog("Video Codec", "The engine used to compress.") }
        resolutionInputLayout.setStartIconOnClickListener { showInfoDialog("Target Resolution", "Changes dimensions.") }
        bitrateInputLayout.setStartIconOnClickListener { showInfoDialog("Video Bitrate", "Data per second.") }
        crfInputLayout.setStartIconOnClickListener { showInfoDialog("CRF", "Quality-based encoding.") }
        presetInputLayout.setStartIconOnClickListener { showInfoDialog("Encoder Preset", "Optimization level.") }
        frameRateInputLayout.setStartIconOnClickListener { showInfoDialog("Frame Rate", "Frames per second.") }
        audioCodecInputLayout.setStartIconOnClickListener { showInfoDialog("Audio Codec", "Audio handling.") }
        
        cropWInputLayout.setStartIconOnClickListener { showInfoDialog("Crop Width", "The width of the final cropped area.") }
        cropHInputLayout.setStartIconOnClickListener { showInfoDialog("Crop Height", "The height of the final cropped area.") }
        cropXInputLayout.setStartIconOnClickListener { showInfoDialog("X Offset", "Horizontal distance to start the crop.") }
        cropYInputLayout.setStartIconOnClickListener { showInfoDialog("Y Offset", "Vertical distance to start the crop.") }
    }

    private fun showInfoDialog(title: String, message: String) {
        MaterialAlertDialogBuilder(requireContext()).setTitle(title).setMessage(message).setPositiveButton("Got it", null).show()
    }

    private fun updateCodecSpecificUi(codec: String) {
        val isHardware = codec.contains("mediacodec")
        bitrateInputLayout.visibility = if (isHardware) View.VISIBLE else View.GONE
        softwareSettingsLayout.visibility = if (isHardware) View.GONE else View.VISIBLE
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.selectedVideoUri.collect { uri ->
                    noVideoPlaceholder.visibility = if (uri == null) View.VISIBLE else View.GONE
                    convertButton.isEnabled = uri != null
                    uri?.let { 
                        setupPlayer(it)
                        viewModel.extractMetadata(requireContext(), it)
                    }
                }}
                launch { viewModel.isAdvancedMode.collect { isAdvanced ->
                    simpleSettingsLayout.visibility = if (isAdvanced) View.GONE else View.VISIBLE
                    advancedSettingsLayout.visibility = if (isAdvanced) View.VISIBLE else View.GONE
                }}
                launch { viewModel.conversionStatus.collect { updateUiForStatus(it) }}
                launch { viewModel.conversionProgress.collect { progressBar.progress = (it * 100).toInt() }}
                launch { viewModel.is10Bit.collect { hdrSwitch.isChecked = it }}
                launch { viewModel.originalMetadata.collect { metadata ->
                    metadataCard.visibility = if (metadata == null) View.GONE else View.VISIBLE
                    metadata?.let {
                        originalCodecText.text = "Codec: ${it.codec}"
                        originalResText.text = "Res: ${it.resolution}"
                        originalBitrateText.text = "Bitrate: ${it.bitrate}"
                        originalFpsText.text = "FPS: ${it.frameRate}"
                    }
                }}
            }
        }
    }

    private fun updateUiForStatus(status: ConversionStatus) {
        when (status) {
            is ConversionStatus.Idle -> {
                progressBar.visibility = View.GONE
                saveToGalleryButton.visibility = View.GONE
                statusTextView.text = if (viewModel.selectedVideoUri.value != null) "Ready to transcode" else "Select a video to begin"
            }
            is ConversionStatus.Converting -> {
                progressBar.visibility = View.VISIBLE
                convertButton.isEnabled = false
                saveToGalleryButton.visibility = View.GONE
                statusTextView.text = "Processing video..."
            }
            is ConversionStatus.Success -> {
                progressBar.visibility = View.GONE
                convertButton.isEnabled = true
                saveToGalleryButton.visibility = View.VISIBLE
                statusTextView.text = "Conversion Complete!"
                com.google.android.material.snackbar.Snackbar.make(requireView(), "Finished successfully", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
            }
            is ConversionStatus.Error -> {
                progressBar.visibility = View.GONE
                convertButton.isEnabled = true
                statusTextView.text = "Error: ${status.message}"
                com.google.android.material.snackbar.Snackbar.make(requireView(), "Conversion failed", com.google.android.material.snackbar.Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun setupPlayer(uri: Uri) {
        player?.release()
        player = ExoPlayer.Builder(requireContext()).build().also {
            playerView.player = it
            it.setMediaItem(MediaItem.fromUri(uri))
            it.prepare()
        }
    }

    private fun appendLog(message: String, level: com.antonkarpenko.ffmpegkit.Level?) {
        activity?.runOnUiThread {
            val color = when (level) {
                com.antonkarpenko.ffmpegkit.Level.AV_LOG_ERROR -> android.graphics.Color.parseColor("#FF5252") // Red
                com.antonkarpenko.ffmpegkit.Level.AV_LOG_WARNING -> android.graphics.Color.parseColor("#FFD740") // Amber
                com.antonkarpenko.ffmpegkit.Level.AV_LOG_DEBUG -> android.graphics.Color.parseColor("#448AFF") // Blue
                else -> android.graphics.Color.parseColor("#CCCCCC") // Light Gray
            }
            
            val spannable = android.text.SpannableString(message)
            spannable.setSpan(android.text.style.ForegroundColorSpan(color), 0, message.length, 0)
            
            logTextView.append(spannable)
            logScrollView.post { logScrollView.fullScroll(View.FOCUS_DOWN) }
        }
    }

    private fun startConversion() {
        val uri = viewModel.selectedVideoUri.value ?: return
        val inputPath = if (uri.scheme == "file") uri.path!! else FFmpegKitConfig.getSafParameterForRead(requireContext(), uri)
        val outputDir = File(requireContext().getExternalFilesDir(null), "Converted")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "converted_${System.currentTimeMillis()}.mp4")
        val outputPath = outputFile.absolutePath

        viewModel.setStatus(ConversionStatus.Converting)
        logTextView.text = ""
        appendLog("Initializing GPU Engine...\n", com.antonkarpenko.ffmpegkit.Level.AV_LOG_INFO)

        val command = viewModel.buildFFmpegCommand(inputPath, outputPath)
        FFmpegKitConfig.enableLogCallback { log -> appendLog(log.message, log.level) }
        FFmpegKit.executeAsync(command) { session ->
            activity?.runOnUiThread {
                if (ReturnCode.isSuccess(session.returnCode)) {
                    viewModel.setStatus(ConversionStatus.Success(outputPath))
                    appendLog("\nConversion Successful!", com.antonkarpenko.ffmpegkit.Level.AV_LOG_INFO)
                } else {
                    viewModel.setStatus(ConversionStatus.Error("FFmpeg failed"))
                    appendLog("\nConversion Failed. Check logs above.", com.antonkarpenko.ffmpegkit.Level.AV_LOG_ERROR)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        player?.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        player?.release()
        player = null
        FFmpegKitConfig.enableLogCallback(null)
    }
}

class SimpleTextWatcher(val onTextChanged: (String) -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { onTextChanged(s.toString()) }
    override fun afterTextChanged(s: android.text.Editable?) {}
}
