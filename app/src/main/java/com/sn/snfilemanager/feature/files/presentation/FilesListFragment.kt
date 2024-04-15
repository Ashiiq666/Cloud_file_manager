package com.sn.snfilemanager.feature.files.presentation

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.sn.mediastorepv.data.ConflictStrategy
import com.sn.snfilemanager.R
import com.sn.snfilemanager.core.base.BaseFragment
import com.sn.snfilemanager.core.extensions.getMimeType
import com.sn.snfilemanager.core.extensions.getUrisForFile
import com.sn.snfilemanager.core.extensions.gone
import com.sn.snfilemanager.core.extensions.infoToast
import com.sn.snfilemanager.core.extensions.observe
import com.sn.snfilemanager.core.extensions.openFile
import com.sn.snfilemanager.core.extensions.openFileWithOtherApp
import com.sn.snfilemanager.core.extensions.shareFiles
import com.sn.snfilemanager.core.extensions.visible
import com.sn.snfilemanager.core.extensions.warningToast
import com.sn.snfilemanager.core.util.MaterialDialogUtils
import com.sn.snfilemanager.core.util.RootPath
import com.sn.snfilemanager.databinding.FragmentFilesListBinding
import com.sn.snfilemanager.feature.files.adapter.FileItemAdapter
import com.sn.snfilemanager.feature.files.data.FabMenu
import com.sn.snfilemanager.feature.files.data.FileModel
import com.sn.snfilemanager.feature.files.data.FileSortOptions
import com.sn.snfilemanager.feature.files.data.toFileModel
import com.sn.snfilemanager.feature.pathpicker.presentation.PathPickerFragment
import com.sn.snfilemanager.job.JobCompletedCallback
import com.sn.snfilemanager.job.JobService
import com.sn.snfilemanager.job.JobType
import com.sn.snfilemanager.providers.preferences.Preferences
import com.sn.snfilemanager.utils.getQuantityString
import com.sn.snfilemanager.view.component.breadcrumb.BreadCrumbItemClickListener
import com.sn.snfilemanager.view.component.breadcrumb.BreadItem
import com.sn.snfilemanager.view.dialog.ConfirmationDialog
import com.sn.snfilemanager.view.dialog.ConflictDialog
import com.sn.snfilemanager.view.dialog.CreateDirectoryDialog
import com.sn.snfilemanager.view.dialog.detail.DetailDialog
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList

@AndroidEntryPoint
class FilesListFragment :
    BaseFragment<FragmentFilesListBinding, FilesListViewModel>(),
    FileItemAdapter.SelectionCallback,
    JobCompletedCallback,
    ActionMode.Callback {
    private val args: FilesListFragmentArgs by navArgs()
    private var adapter: FileItemAdapter? = null
    private var actionMode: ActionMode? = null
    private lateinit var materialDialogUtils: MaterialDialogUtils
    private var isSortAscending: Boolean = true

    override fun getViewModelClass() = FilesListViewModel::class.java

    override fun getViewBinding() = FragmentFilesListBinding.inflate(layoutInflater)

    override fun getMenuResId(): Int = R.menu.menu_files

    override fun getToolbar(): Toolbar = binding.toolbar

    override fun onMenuItemSelected(menuItemId: Int) =
        when (menuItemId) {
            R.id.action_search -> {
                initSearch()
                true
            }

//            R.id.create_folder -> {
//                viewModel.currentPath?.let { path ->
//                    showCreateDirectoryDialog(path)
//                }
//                true
//            }

            else -> super.onMenuItemSelected(menuItemId)
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

    override fun onDestroyActionMode(mode: ActionMode?) {
        actionMode = null
        clearSelection()
    }

    override fun onActionItemClicked(
        mode: ActionMode?,
        item: MenuItem?,
    ): Boolean {
        checkActionMenuStatus()
        when (item?.itemId) {
            R.id.action_copy -> {
                viewModel.isCopy = true
                showPathSelectionDialog()
            }

            R.id.action_move -> {
                viewModel.isCopy = false
                showPathSelectionDialog()
            }

            R.id.action_delete -> {
                actionDelete()
            }

            R.id.action_share -> {
                actionShare()
            }

            R.id.action_open_with -> {
                actionOpenWith()
            }

            R.id.action_detail -> {
                actionDetail()
            }
        }

        return true
    }

    override fun setupViews() {
        binding.vm = viewModel
        materialDialogUtils = MaterialDialogUtils()

        binding.lifecycleOwner = viewLifecycleOwner
        initAdapter()
        handleBackPressed()
        initBreadListener()
        initFirstList()
        initFabClick()
    }

    override fun onStartSelection() {
        (activity as? AppCompatActivity)?.startSupportActionMode(this)
    }

    override fun onEndSelection() {
        actionMode?.finish()
    }

    override fun onUpdateSelection(selectedSize: Int) {
        actionMode?.title = selectedSize.toString()
    }

    override fun scannedOnCompleted() {
        // scanned completed
    }

    override fun <T> jobOnCompleted(
        jobType: JobType,
        data: List<T>?,
    ) {
        when (jobType) {
            JobType.COPY -> {
                viewModel.currentPath?.let { path ->
                    viewModel.getFilesList(path)
                }
            }

            JobType.DELETE -> {
                activity?.runOnUiThread {
                    data?.filterIsInstance<FileModel>()?.let { adapter?.removeItems(it) }
                    adapter?.getItems()?.let { viewModel.setUpdateList(it) }
                }
            }

            JobType.CREATE -> {
                activity?.runOnUiThread {
                    data?.filterIsInstance<Path>()?.firstOrNull()?.toFileModel()?.let { file ->
                        adapter?.addItem(file)
                        adapter?.getItems()?.let { viewModel.setUpdateList(it) }
                    }
                }
            }
        }
        activity?.runOnUiThread { context?.infoToast(getString(R.string.completed)) }
    }

    override fun observeData() {
        observe(viewModel.conflictQuestionLiveData) { event ->
            event.getContentIfNotHandled()?.let { fileName ->
                ConflictDialog(requireContext(), fileName).apply {
                    onSelected = { strategy: ConflictStrategy, isAll: Boolean ->
                        viewModel.conflictDialogDeferred.complete(Pair(strategy, isAll))
                    }
                    onDismiss = { actionMode?.finish() }
                }.show()
            }
        }
        observe(viewModel.startMoveJobLiveData) { event ->
            event.getContentIfNotHandled()?.let { data ->
                if (data.first.isNotEmpty()) {
                    actionMode?.finish()
                    startCopyService(data.first, data.second)
                }
            }
        }
        observe(viewModel.startDeleteJobLiveData) { event ->
            event.getContentIfNotHandled()?.let { data ->
                actionMode?.finish()
                startDeleteService(data)
            }
        }
        observe(viewModel.startCreateFolderJob) { event ->
            event.getContentIfNotHandled()?.let { path ->
                startCreateDirectory(path)
            }
        }
        observe(viewModel.updateListLiveData) { event ->
            event.getContentIfNotHandled()?.let { list ->
                adapter?.setItems(list)

            }
        }
        observe(viewModel.pathConflictLiveData) { event ->
            event.getContentIfNotHandled()?.let {
                context?.warningToast(getString(R.string.path_conflict_warning))
            }
        }
        observe(viewModel.searchStateLiveData) { event ->
            event.getContentIfNotHandled()?.let { stateAndProgress ->
                if (stateAndProgress.first) {
                    binding.rcvFiles.gone()
                } else {
                    binding.rcvFiles.visible()
                }
            }
        }
    }


    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_files, menu)
            }

            override fun onPrepareMenu(menu: Menu) {
                super.onPrepareMenu(menu)

                val itemSortByName = menu.findItem(R.id.action_sort_by_name)
                val itemSortByType = menu.findItem(R.id.action_sort_by_type)
                val itemSortBySize = menu.findItem(R.id.action_sort_by_size)
                val itemSortByLastModified = menu.findItem(R.id.action_sort_by_last_modified)
                val itemSortOrderAscending = menu.findItem(R.id.action_sort_order_ascending)
                val itemSortDirectoriesFirst = menu.findItem(R.id.action_sort_directories_first)
                val itemShowHiddenFiles = menu.findItem(R.id.action_show_hidden_files)


                itemSortOrderAscending.isChecked =
                    (Preferences.Popup.orderFiles == FileSortOptions.Order.ASCENDING)
                itemSortDirectoriesFirst.isChecked = Preferences.Popup.isDirectoriesFirst
                itemShowHiddenFiles.isChecked = Preferences.Popup.showHiddenFiles
                val quantity = if (Preferences.Popup.isGridEnabled) 1 else 2
                val titleToggleGrid =
                    getQuantityString(R.plurals.file_list_action_toggle_grid, quantity)
                val itemToggleGrid = menu.findItem(R.id.action_toggle_grid)

                val menuItems: MutableList<MenuItem> = ArrayList()
                itemSortByName?.let { menuItems.add(it) }
                itemSortByType?.let { menuItems.add(it) }
                itemSortBySize?.let { menuItems.add(it) }
                menuItems.add(itemSortByLastModified)

                val checkedSortByItem = when (Preferences.Popup.sortBy) {
                    FileSortOptions.SortBy.NAME -> itemSortByName
                    FileSortOptions.SortBy.TYPE -> itemSortByType
                    FileSortOptions.SortBy.SIZE -> itemSortBySize
                    FileSortOptions.SortBy.LAST_MODIFIED -> itemSortByLastModified
                }
                checkedSortByItem.isChecked = true


            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                handleMenuItemSelected(menuItem)
                requireActivity().invalidateMenu()
                return true
            }

        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }


    private fun handleMenuItemSelected(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.action_toggle_grid -> {
                viewModel.setGriToggle()
            }

            R.id.action_sort_by_name -> {
                viewModel.setSortBy(FileSortOptions.SortBy.NAME)
            }

            R.id.action_sort_by_type -> {
                viewModel.setSortBy(FileSortOptions.SortBy.TYPE)
            }

            R.id.action_sort_by_size -> {
                viewModel.setSortBy(FileSortOptions.SortBy.SIZE)
            }

            R.id.action_sort_by_last_modified -> {
                viewModel.setSortBy(FileSortOptions.SortBy.LAST_MODIFIED)
            }

            R.id.action_sort_order_ascending -> {
                viewModel.setOrderFiles()
            }

            R.id.action_sort_directories_first -> {
                viewModel.setDirectoriesFirst()
            }

            R.id.action_refresh -> {
                refresh()
            }

            R.id.action_select_all -> {

            }

            else -> {}
        }

    }

    fun refresh() {
       // viewModel.reload()
    }

    private fun initFabClick() {
        val mFab = binding.mfab
        val mFabCreateFile = binding.fabCreateFile
        val mFabCreateFolder = binding.fabCreateFolder
        val fabMenu = FabMenu(requireContext(), mFab, mFabCreateFile, mFabCreateFolder)

        mFab.setOnClickListener {
            fabMenu.toggle()
        }

        mFabCreateFile.setOnClickListener {
            fabMenu.toggle()
            showCreateDialog(viewModel.currentPath ?: "", true)
        }

        mFabCreateFolder.setOnClickListener {
            fabMenu.toggle()
            viewModel.currentPath?.let { path ->
                showCreateDirectoryDialog(path)
            }
            true
        }
    }

    private fun showCreateDialog(path: String, isCreatingFile: Boolean) {
        CreateDirectoryDialog(
            path = path,
            isCreatingFile = isCreatingFile,
            onCreate = { name ->
                if (isCreatingFile) {
                    viewModel.createFile(name)
                } else {
                    createFile(name)
                }
            }
        ).show(childFragmentManager, CreateDirectoryDialog.TAG)
    }


    private fun createFile(filePath: Path) {
        viewModel.createFile(filePath)
    }

    private fun initFirstList() {
        if (!viewModel.firstInit) {
            updateFileList(viewModel.getStoragePath(args.storageArgs))
            val rootBread =
                when (args.storageArgs) {
                    RootPath.EXTERNAL -> getString(R.string.external_storage)
                    RootPath.INTERNAL -> getString(R.string.internal)
                    else -> {
                        getString(R.string.external_storage)}
                }
            binding.breadcrumbBar.addBreadCrumbItem(BreadItem(rootBread, null))
        }
    }

    private fun updateFileList(path: String) {
        with(viewModel) {
            firstInit = true
            viewModel.cancelFileListJob()
            updateDirectoryList(path)
            getFilesList(path)
        }
    }

    private fun addPathItem(item: BreadItem) {
        item.path?.let {
            if (File(it).isDirectory) {
                binding.breadcrumbBar.addBreadCrumbItem(item)
            }
        }
    }

    private fun initBreadListener() {
        binding.breadcrumbBar.setListener(
            object : BreadCrumbItemClickListener {
                override fun onItemClick(
                    item: BreadItem,
                    position: Int,
                ) {
                    val path =
                        if (position == 0 || item.path == null) {
                            viewModel.getStoragePath(args.storageArgs)
                        } else {
                            item.path
                        }
                    updateFileList(path)
                    binding.breadcrumbBar.removeItemsWithRange(position)
                    viewModel.updateDirectoryListWithPos(position)
                }
            },
        )
    }

    private fun checkActionMenuStatus() {
        if (viewModel.selectedItemsContainsFolder()) {
            actionMode?.menu?.findItem(R.id.action_share)?.isVisible = false
            actionMode?.menu?.findItem(R.id.action_open_with)?.isVisible = false
        } else {
            actionMode?.menu?.findItem(R.id.action_share)?.isVisible = true
            actionMode?.menu?.findItem(R.id.action_open_with)?.isVisible =
                viewModel.isSingleItemSelected()
        }
    }

    private fun initAdapter() {
        if (adapter == null) {
            adapter =
                FileItemAdapter(
                    requireContext(),
                    onSelected = { model, selected ->
                        viewModel.addSelectedItem(model, selected)
                        checkActionMenuStatus()
                    },
                    onClick = { model ->
                        if (model.isDirectory) {
                            if (Files.isReadable(Paths.get(model.absolutePath))) {
                                updateFileList(model.absolutePath)
                                addPathItem(BreadItem(model.name, model.absolutePath))
                            } else {
                                context?.warningToast(getString(R.string.folder_permission_info))
                            }
                        } else {
                            openFile(model)
                        }
                    },
                    selectionCallback = this@FilesListFragment,
                )
        }
        binding.rcvFiles.adapter = adapter
        binding.rcvFiles.itemAnimator = null
    }

    private fun clearSelection() {
        adapter?.finishSelectionAndReset()
        viewModel.clearSelectionList()
    }

    private fun openFile(file: FileModel) {
        file.absolutePath.getMimeType()?.let { mime ->
            context?.openFile(file.absolutePath, mime)
        }
    }

    private fun showPathSelectionDialog() {
        PathPickerFragment(pathCallback = { path ->
            if (path.isNullOrEmpty()) {
                actionMode?.finish()
                context?.infoToast(getString(R.string.path_not_selected))
            } else {
                viewModel.moveFilesAndDirectories(Paths.get(path))
            }
        }).show(
            childFragmentManager,
            DetailDialog.TAG,
        )
    }

    private fun showCreateDirectoryDialog(path: String, isCreatingFile: Boolean = false) {
        CreateDirectoryDialog(
            path = path,
            isCreatingFile = isCreatingFile,
            onCreate = { folderName ->
                if (isCreatingFile) {
                    viewModel.createFile(folderName)
                } else {
                    viewModel.createFolder(folderName)
                }
            }).show(childFragmentManager, CreateDirectoryDialog.TAG)
    }

    private fun actionDetail() {
        DetailDialog(requireContext(), viewModel.getSelectedItem()).show(
            childFragmentManager,
            DetailDialog.TAG,
        )
    }

    private fun actionOpenWith() {
        viewModel.getSelectedItem().firstOrNull()?.let { selectedItem ->
            selectedItem.absolutePath.getMimeType()
                ?.let { context?.openFileWithOtherApp(selectedItem.absolutePath, it) }
        }
    }

    private fun actionShare() {
        val files = viewModel.getSelectedItemToFiles()
        val uris = context?.getUrisForFile(files)
        uris?.let {
            context?.shareFiles(it)
        }
    }

    private fun isAscending(): Boolean = isSortAscending



    private fun actionDelete() {
        ConfirmationDialog(
            requireContext(),
            getString(R.string.are_you_sure),
            getString(R.string.delete_warning),
        ).apply {
            onSelected = { selected ->
                if (selected) {
                    viewModel.deleteFiles()
                } else {
                    actionMode?.finish()
                }
            }
        }.show()
    }

    private fun startCopyService(
        operationItemList: List<FileModel>,
        destinationPath: Path,
    ) {
        JobService.copy(
            operationItemList,
            destinationPath,
            viewModel.isCopy,
            this@FilesListFragment,
            requireContext(),
        )
    }

    private fun startDeleteService(operationItemList: List<FileModel>) {
        JobService.delete(
            operationItemList,
            this@FilesListFragment,
            requireContext(),
        )
    }

    private fun startCreateDirectory(destinationPath: Path) {
        JobService.createDirectory(
            destinationPath,
            this@FilesListFragment,
            requireContext(),
        )
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

    private fun handleBackPressed() {
        val directoryList = viewModel.getDirectoryList()
        val callback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (directoryList.size > 1) {
                        directoryList.removeAt(directoryList.lastIndex)
                        updateFileList(directoryList.last())
                        binding.breadcrumbBar.removeLastBreadCrumbItem()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
    }
}
