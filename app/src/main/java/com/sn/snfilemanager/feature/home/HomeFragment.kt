package com.sn.snfilemanager.feature.home

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore.*
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getColor
import androidx.recyclerview.widget.LinearLayoutManager
import com.sn.mediastorepv.data.MediaType
import com.sn.snfilemanager.R
import com.sn.snfilemanager.core.base.BaseFragment
import com.sn.snfilemanager.core.extensions.click
import com.sn.snfilemanager.core.extensions.getPackage
import com.sn.snfilemanager.core.extensions.infoToast
import com.sn.snfilemanager.core.util.DocumentType
import com.sn.snfilemanager.core.util.FileUtils
import com.sn.snfilemanager.core.util.RootPath
import com.sn.snfilemanager.databinding.FragmentHomeBinding
import com.sn.snfilemanager.feature.files.adapter.FileItemAdapter
import com.sn.snfilemanager.feature.files.adapter.RecentImagemodelAdapter
import com.sn.snfilemanager.feature.files.data.RecentFile
import com.sn.snfilemanager.view.dialog.ConfirmationDialog
import com.sn.snfilemanager.view.dialog.permission.PermissionDialog
import com.sn.snfilemanager.view.dialog.permission.PermissionDialogType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(), ActionMode.Callback {
    private var permissionDialog: PermissionDialog? = null
    private var confirmationDialog: ConfirmationDialog? = null
    private val BASE_PATH = "/storage/emulated/0"
    private lateinit var fileUtils: FileUtils
    private var actionMode: ActionMode? = null
    private var adapter: FileItemAdapter? = null

    override fun getViewModelClass() = HomeViewModel::class.java

    override fun getViewBinding() = FragmentHomeBinding.inflate(layoutInflater)

    override fun getMenuResId(): Int = R.menu.menu_home

    override fun getToolbar(): Toolbar = binding.toolbar


    override fun setupViews() {
        initMenuButtonListener()
    }

    override fun onResume() {
        super.onResume()
        initPermission()
        setStatusBarColor()
        setStorageSpaceInGB()
        fileUtils = FileUtils()
        setRecentImages()
        updateMediaCounts()
    }


    override fun onMenuItemSelected(menuItemId: Int) =
        when (menuItemId) {
            R.id.store -> {
                // context?.openUrl(Constant.STORE_URL)
                true
            }

            R.id.action_search -> {
                initSearch()
                true
            }

            else -> super.onMenuItemSelected(menuItemId)
        }


    override fun observeData() {
//        observe(viewModel.availableStorageLiveData) { memory ->
//            binding.btnFile.subTitle = getString(R.string.available_storage, memory)
//        }
//        observe(viewModel.availableExternalStorageLiveData) { memory ->
//            binding.btnExternalFile.subTitle =
//                memory?.let { getString(R.string.available_storage, it) }
//                    ?: getString(R.string.no_external_storage)
//        }
    }

    private fun allowStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            routeFileAccessSettings()
        } else {
            if (viewModel.hasStorageRequestedPermissionBefore()) {
                routeAppSettings()
            } else {
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        viewModel.setStoragePermissionRequested()
    }

    fun setStorageSpaceInGB() {
        val pbSpace = binding.pbSpace

        val totalSpace = getStorageSpaceInGB(SpaceType.TOTAL)
        val usedSpace = getStorageSpaceInGB(SpaceType.USED)
        val totalFreeSpace = totalSpace - usedSpace // Calculate total free space

        val animation = ObjectAnimator.ofInt(pbSpace, "progress", 0, usedSpace)

        val formattedUsedSpace = String.format("%d", usedSpace)
        val formattedTotalSpace = String.format("%d", totalSpace)
        val formattedFreeSpace = String.format("Free space: %d GB", totalFreeSpace)




        binding.tvSpaceFree.text = formattedFreeSpace
        binding.tvSpaceOf.text =
            getString(R.string.space_of_format, formattedUsedSpace, formattedTotalSpace)

        animation.duration = 1000
        animation.start()

        binding.pbSpace.progress = 0
        animation.duration = 1000
        animation.addUpdateListener { valueAnimator ->
            binding.pbSpace.progress = valueAnimator.animatedValue as Int
        }
        animation.start()
    }

    fun getStorageSpaceInGB(spaceType: SpaceType): Int {
        val file = File(BASE_PATH)
        val totalSpace = file.totalSpace
        val freeSpace = file.freeSpace
        val usedSpace = totalSpace - freeSpace

        return when (spaceType) {
            SpaceType.TOTAL -> convertBytesToGB(totalSpace)
            SpaceType.FREE -> convertBytesToGB(freeSpace)
            SpaceType.USED -> convertBytesToGB(usedSpace)
        }
    }

    private fun convertBytesToGB(bytes: Long): Int {
        val gb = bytes / (1024 * 1024 * 1024)
        return gb.toInt()
    }

    enum class SpaceType {
        TOTAL,
        FREE,
        USED
    }


    private fun initSearch() {
        binding.toolbar.menu?.findItem(R.id.action_search)?.let { item ->
            val searchView = item.actionView as? SearchView
            searchView?.queryHint = getString(R.string.search_hint)
            searchView?.setOnQueryTextListener(
                object :
                    SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.searchFiles(query)
                        return true
                    }

                    override fun onQueryTextChange(newText: String?): Boolean {
                        viewModel.searchFiles(newText)
                        return true
                    }
                },
            )

            item.setOnActionExpandListener(
                object : MenuItem.OnActionExpandListener {
                    override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                        return true
                    }

                    override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                        return if (selectionIsActive()) {
                            actionMode?.finish()
                            false
                        } else {
                            true
                        }
                    }
                },
            )
        }
    }

    private fun selectionIsActive(): Boolean = adapter?.selectionIsActive() ?: false


    private fun allowNotificationPermission() {
        if (!viewModel.hasNotificationRequestedPermissionBefore()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            routeNotificationSettings()
        }
        viewModel.setNotificationPermissionRequested()
    }

    private fun initPermission() {
        val type =
            if (viewModel.hasStorageRequestedPermissionBefore()) PermissionDialogType.WARNING else PermissionDialogType.DEFAULT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                showPermissionDialog(type)
            } else {
                initNotificationPermission()
            }
        } else if (!checkStoragePermission(requireContext())) {
            showPermissionDialog(type)
        } else {
            initNotificationPermission()
        }
    }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showPermissionDialog(PermissionDialogType.WARNING)
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                showNotificationDialog()
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun routeFileAccessSettings() {
        val intent =
            Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse(context?.getPackage()),
            )
        startActivity(intent)
    }

    private fun routeAppSettings() {
        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse(context?.getPackage()),
            )
        startActivity(intent)
    }

    private fun routeNotificationSettings() {
        val notificationManager = NotificationManagerCompat.from(requireContext())
        if (!notificationManager.areNotificationsEnabled()) {
            val settingsIntent =
                Intent().apply {
                    action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                    putExtra(Settings.EXTRA_APP_PACKAGE, context?.packageName)
                }
            context?.startActivity(settingsIntent)
        }
    }

    private fun showPermissionDialog(type: PermissionDialogType = PermissionDialogType.DEFAULT) {
        if (permissionDialog == null || permissionDialog?.isShowing == false) {
            permissionDialog =
                PermissionDialog(requireContext(), type).apply {
                    onAllow = { allowStoragePermission() }
                }
            permissionDialog?.show()
        }
    }

    private fun showNotificationDialog() {
        if (confirmationDialog == null || confirmationDialog?.isShowing == false) {
            confirmationDialog =
                ConfirmationDialog(
                    requireContext(),
                    getString(R.string.permission_warning_title),
                    getString(R.string.notification_permission_info),
                ).apply {
                    onSelected = { ok ->
                        if (ok) {
                            allowNotificationPermission()
                        }
                    }
                }
            confirmationDialog?.show()
        }
    }

    private fun checkStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun initNotificationPermission() {
        if (!viewModel.notificationRuntimeRequested) {
            if (!checkNotificationPermission(requireContext())) {
                showNotificationDialog()
            }
            viewModel.notificationRuntimeRequested = true
        }
    }

    private fun checkNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun setStatusBarColor() {
        binding.appBar.setBackgroundColor(getColor(requireContext(), R.color.bg_color))
    }

    private fun initMenuButtonListener() {
        with(binding) {
            ibImages.click {
                navigate(
                    HomeFragmentDirections.actionHomeMedia(
                        mediaType = MediaType.IMAGES,
                        title = getString(R.string.images),
                    ),
                )
            }
            ibVideos.click {
                navigate(
                    HomeFragmentDirections.actionHomeMedia(
                        mediaType = MediaType.VIDEOS,
                        getString(R.string.videos),
                    ),
                )
            }
            ibSounds.click {
                navigate(
                    HomeFragmentDirections.actionHomeMedia(
                        mediaType = MediaType.AUDIOS,
                        title = getString(R.string.sounds),
                    ),
                )
            }
            ibDocuments.click {
                navigate(
                    HomeFragmentDirections.actionHomeMedia(
                        mediaType = MediaType.FILES,
                        title = getString(R.string.documents),
                    ),
                )
            }
            ibApks.click {
                navigate(
                    HomeFragmentDirections.actionHomeMedia(
                        mediaType = MediaType.FILES,
                        documentType = DocumentType.APK.name,
                        title = getString(R.string.apk_files),
                    ),
                )
            }
            ibArchives.click { // Change this id to your Downloads button id if it's different
                ibArchives.click {
                    navigate(
                        HomeFragmentDirections.actionHomeMedia(
                            mediaType = MediaType.DOWNLOADS,
                            title = getString(R.string.downloads)
                        )
                    )
                }

            }
            btnFile.click {
                navigate(
                    HomeFragmentDirections.actionHomeFile(
                        storageArgs = RootPath.INTERNAL,
                        title = getString(R.string.folders),
                    ),
                )
            }
            btnExternalFile.click {  //todo: remove comment
                context?.infoToast("This feature is not supported yet.")

                viewModel.availableExternalStorageLiveData.value?.let {
                    navigate(
                        HomeFragmentDirections.actionHomeFile(
                            storageArgs = RootPath.EXTERNAL,
                            title = getString(R.string.folders),
                        ),
                    )
                }
            }
            btnSettings.click {
                navigate(HomeFragmentDirections.actionSettings())
            }
            btnAbout.click {
                navigate(HomeFragmentDirections.actionAbout())
            }

            btnViewAll.click {
                navigate(
                    HomeFragmentDirections.actionHomeMedia(
                        mediaType = MediaType.IMAGES,
                        title = getString(R.string.all_files),
                    )
                )
            }
        }


    }

    private fun setRecentImages() {
        CoroutineScope(Dispatchers.Main).launch {
            val recentFiles = withContext(Dispatchers.IO) {
                // Retrieve your recent files here
                fileUtils.getRecentFiles(requireContext())
            }
            val adapter = RecentImagemodelAdapter(recentFiles, requireContext()) { file ->
                openFile(file)
            }

            binding.recyRecentsImages.layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            binding.recyRecentsImages.adapter = adapter
        }
    }


    private fun updateMediaCounts() {
        CoroutineScope(Dispatchers.IO).launch {
            val imageCount = queryMediaCount(Images.Media.EXTERNAL_CONTENT_URI)
            val videoCount = queryMediaCount(Video.Media.EXTERNAL_CONTENT_URI)
            val audioCount = queryMediaCount(Audio.Media.EXTERNAL_CONTENT_URI)
            val documentCount = queryDocumentCount()
            val apkCount = queryApkCount()
            val downloadCount = queryMediaCount(Downloads.EXTERNAL_CONTENT_URI)
            // Add more counts for other media types if needed

            withContext(Dispatchers.Main) {
                binding.tvImageCount.text = getString(R.string.files_count, imageCount)
                binding.tvVideosCount.text = getString(R.string.files_count, videoCount)
                binding.tvAudioCount.text = getString(R.string.files_count, audioCount)
                binding.tvDocumentsCount.text = getString(R.string.files_count, documentCount)
                binding.tvApkCount.text = getString(R.string.files_count, apkCount)
                binding.tvDownloadCount.text = getString(R.string.files_count, downloadCount)
                // Update UI for other counts
            }
        }
    }

    private suspend fun queryMediaCount(uri: Uri): Int = withContext(Dispatchers.IO) {
        val cursor = requireContext().contentResolver.query(uri, null, null, null, null)
        val count = cursor?.count ?: 0
        cursor?.close()
        return@withContext count
    }

    private suspend fun queryApkCount(): Int = withContext(Dispatchers.IO) {
        val selection = Files.FileColumns.MIME_TYPE + "=?"
        val selectionArgs = arrayOf("application/vnd.android.package-archive")
        requireContext().contentResolver.query(
            Files.getContentUri("external"),
            null,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            return@withContext cursor.count
        } ?: return@withContext 0
    }

    private fun clearSelection() {
        adapter?.finishSelectionAndReset()
        viewModel.clearSelectionList()
    }

    private suspend fun queryDocumentCount(): Int = withContext(Dispatchers.IO) {
        // Define common document MIME types or use a pattern like "application/%"
        val documentMimeTypes = arrayOf(
            "application/pdf",
            "application/msword",
            "application/vnd.ms-excel", /* Add more as needed */
        )
        val mimeTypesSelection =
            documentMimeTypes.joinToString(separator = " OR ") { Files.FileColumns.MIME_TYPE + "=?" }

        requireContext().contentResolver.query(
            Files.getContentUri("external"),
            null,
            mimeTypesSelection,
            documentMimeTypes,
            null
        )?.use { cursor ->
            return@withContext cursor.count
        } ?: return@withContext 0
    }


    private fun openFile(file: RecentFile) {
        // Example function to open a file. Implement based on your requirements.
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(file.uri, file.mimeType)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(intent)
    }

    override fun onCreateActionMode(
        mode: ActionMode?,
        menu: Menu?,
    ): Boolean {
        actionMode = mode
        mode?.menuInflater?.inflate(R.menu.menu_action, menu)
        return true
    }

    override fun onPrepareActionMode(
        mode: ActionMode?,
        menu: Menu?,
    ): Boolean {
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.action_delete -> {
                // Implement your delete action here
                clearSelection()
                mode?.finish()
                true
            }

            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
        clearSelection()
    }
}
