package com.renskylab.videoconverter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class ImportedFragment : Fragment() {
    private val viewModel: ConverterViewModel by activityViewModels()
    private lateinit var adapter: VideoAdapter
    private lateinit var emptyStateView: View
    private lateinit var recyclerView: RecyclerView
    private var isGrid = false

    private val selectVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handleImport(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val prefs = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
        isGrid = prefs.getBoolean("imported_is_grid", false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_imported, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.setSupportActionBar(toolbar)
        
        emptyStateView = view.findViewById(R.id.emptyStateView)
        recyclerView = view.findViewById(R.id.importedRecyclerView)
        
        setupRecyclerView()
        view.findViewById<ExtendedFloatingActionButton>(R.id.addVideoFab).setOnClickListener { selectVideoLauncher.launch("video/*") }
        
        lifecycleScope.launch { 
            viewModel.importedVideos.collect { 
                adapter.submitList(it)
                val isEmpty = it.isEmpty()
                emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            } 
        }
        refreshLists()
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter(
            onItemClick = { file ->
                viewModel.setSelectedVideo(Uri.fromFile(file))
                findNavController().navigate(R.id.converterFragment)
            },
            onActionClick = { file, anchor -> showMenu(file, anchor, isImported = true) },
            onLongClick = { file, anchor -> showMenu(file, anchor, isImported = true) }
        )
        recyclerView.adapter = adapter
        updateLayoutManager()
    }

    private fun updateLayoutManager() {
        adapter.isGridLayout = isGrid
        recyclerView.layoutManager = if (isGrid) GridLayoutManager(requireContext(), 2) else LinearLayoutManager(requireContext())
        adapter.notifyDataSetChanged()
        activity?.invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library_menu, menu)
        val toggleItem = menu.findItem(R.id.action_layout_toggle)
        toggleItem.setIcon(if (isGrid) R.drawable.ic_view_list else R.drawable.ic_view_grid)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_layout_toggle) {
            isGrid = !isGrid
            requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("imported_is_grid", isGrid).apply()
            updateLayoutManager()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun playVideo(file: File) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("video_uri", Uri.fromFile(file).toString())
        }
        startActivity(intent)
    }

    private fun showMenu(file: File, anchor: View, isImported: Boolean) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Play")
        popup.menu.add("Load in Converter")
        popup.menu.add("Delete")
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Play" -> { playVideo(file); true }
                "Load in Converter" -> {
                    viewModel.setSelectedVideo(Uri.fromFile(file))
                    findNavController().navigate(R.id.converterFragment)
                    true
                }
                "Delete" -> { file.delete(); refreshLists(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun handleImport(uri: Uri) {
        lifecycleScope.launch {
            val importedDir = File(requireContext().getExternalFilesDir(null), "Imported")
            if (!importedDir.exists()) importedDir.mkdirs()
            val fileName = "imported_${System.currentTimeMillis()}.mp4"
            val destFile = File(importedDir, fileName)
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            refreshLists()
            com.google.android.material.snackbar.Snackbar.make(requireView(), "Imported successfully", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun refreshLists() {
        val importedDir = File(requireContext().getExternalFilesDir(null), "Imported")
        val convertedDir = File(requireContext().getExternalFilesDir(null), "Converted")
        viewModel.refreshVideoLists(importedDir, convertedDir)
    }
}

class ConvertedFragment : Fragment() {
    private val viewModel: ConverterViewModel by activityViewModels()
    private lateinit var adapter: VideoAdapter
    private lateinit var emptyStateView: View
    private lateinit var recyclerView: RecyclerView
    private var isGrid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val prefs = requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE)
        isGrid = prefs.getBoolean("converted_is_grid", false)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_converted, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.setSupportActionBar(toolbar)
        
        emptyStateView = view.findViewById(R.id.emptyStateView)
        recyclerView = view.findViewById(R.id.convertedRecyclerView)
        
        setupRecyclerView()
        
        lifecycleScope.launch { 
            viewModel.convertedVideos.collect { 
                adapter.submitList(it)
                val isEmpty = it.isEmpty()
                emptyStateView.visibility = if (isEmpty) View.VISIBLE else View.GONE
                recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
            } 
        }
        refreshLists()
    }

    private fun setupRecyclerView() {
        adapter = VideoAdapter(
            onItemClick = { playVideo(it) },
            onActionClick = { file, anchor -> showMenu(file, anchor) },
            onLongClick = { file, anchor -> showMenu(file, anchor) }
        )
        recyclerView.adapter = adapter
        updateLayoutManager()
    }

    private fun updateLayoutManager() {
        adapter.isGridLayout = isGrid
        recyclerView.layoutManager = if (isGrid) GridLayoutManager(requireContext(), 2) else LinearLayoutManager(requireContext())
        adapter.notifyDataSetChanged()
        activity?.invalidateOptionsMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.library_menu, menu)
        val toggleItem = menu.findItem(R.id.action_layout_toggle)
        toggleItem.setIcon(if (isGrid) R.drawable.ic_view_list else R.drawable.ic_view_grid)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_layout_toggle) {
            isGrid = !isGrid
            requireContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("converted_is_grid", isGrid).apply()
            updateLayoutManager()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun playVideo(file: File) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("video_uri", Uri.fromFile(file).toString())
        }
        startActivity(intent)
    }

    private fun showMenu(file: File, anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add("Play")
        popup.menu.add("Share")
        popup.menu.add("Export to Gallery")
        popup.menu.add("Delete")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Play" -> { playVideo(file); true }
                "Share" -> { shareVideo(file); true }
                "Export to Gallery" -> {
                    (activity as? MainActivity)?.exportVideoToGallery(file)
                    com.google.android.material.snackbar.Snackbar.make(requireView(), "Saving to Gallery...", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                    true
                }
                "Delete" -> { file.delete(); refreshLists(); true }
                else -> false
            }
        }
        popup.show()
    }

    private fun shareVideo(file: File) {
        val uri = androidx.core.content.FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Video"))
    }

    private fun refreshLists() {
        val importedDir = File(requireContext().getExternalFilesDir(null), "Imported")
        val convertedDir = File(requireContext().getExternalFilesDir(null), "Converted")
        viewModel.refreshVideoLists(importedDir, convertedDir)
    }
}
