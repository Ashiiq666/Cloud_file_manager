package com.sn.snfilemanager.feature.media.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sn.mediastorepv.data.Media
import com.sn.mediastorepv.data.MediaType
import com.sn.snfilemanager.BR
import com.sn.snfilemanager.R
import com.sn.snfilemanager.core.extensions.click
import com.sn.snfilemanager.core.extensions.gone
import com.sn.snfilemanager.core.extensions.invisible
import com.sn.snfilemanager.core.extensions.loadWithGlide
import com.sn.snfilemanager.core.extensions.setMargins
import com.sn.snfilemanager.core.extensions.visible
import com.sn.snfilemanager.core.util.FileExtension
import com.sn.snfilemanager.databinding.ItemAudioBinding
import com.sn.snfilemanager.databinding.ItemDocumentBinding
import com.sn.snfilemanager.databinding.ItemDownloadsBinding
import com.sn.snfilemanager.databinding.ItemImagesBinding
import com.sn.snfilemanager.databinding.ItemVideoBinding

class MediaItemAdapter(
    private val onClick: ((Media) -> Unit)? = null,
    private val onSelected: ((Media, Boolean) -> Unit)? = null,
    private val selectionCallback: SelectionCallback? = null,
) : RecyclerView.Adapter<MediaItemAdapter.AutoCompleteViewHolder>() {
    private val selectedItems: MutableList<Media> = mutableListOf()
    private var isSelectionModeActive = false
    private var mediaItems: List<Media> = emptyList()

    fun setItems(newItems: List<Media>) {
        val diffResult = DiffUtil.calculateDiff(MediaDiffCallback(mediaItems, newItems))
        mediaItems = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    fun removeItems(mediaToRemove: List<Media>) {
        for (fileToRemove in mediaToRemove) {
            val position = mediaItems.indexOf(fileToRemove)
            if (position != RecyclerView.NO_POSITION) {
                mediaItems = mediaItems.toMutableList().apply { removeAt(position) }
                notifyItemRemoved(position)
            }
        }
    }

    fun finishSelectionAndReset() {
        for (selectedItem in selectedItems) {
            val position = mediaItems.indexOf(selectedItem)
            if (position != RecyclerView.NO_POSITION) {
                selectedItem.isSelected = false
                notifyItemChanged(position)
            }
        }
        selectedItems.clear()
        isSelectionModeActive = false
        selectionCallback?.onEndSelection()
    }

    fun getSelectedItems(): MutableList<Media> = selectedItems

    fun selectionIsActive(): Boolean = isSelectionModeActive

    override fun getItemViewType(position: Int): Int {
        val item = mediaItems[position]
        return when (item.mediaType) {
            MediaType.IMAGES -> R.layout.item_images
            MediaType.AUDIOS -> R.layout.item_audio
            MediaType.VIDEOS -> R.layout.item_video
            MediaType.FILES -> R.layout.item_document
            MediaType.DOWNLOADS -> R.layout.item_downloads // Add this line, assuming you have such a layout
            else -> throw RuntimeException("invalid object")
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AutoCompleteViewHolder {
        return AutoCompleteViewHolder.create(
            LayoutInflater.from(parent.context),
            parent,
            viewType,
            this@MediaItemAdapter,
        )
    }

    override fun onBindViewHolder(
        holder: AutoCompleteViewHolder,
        position: Int,
    ) {
        holder.bindItem(mediaItems[position])
    }

    override fun getItemCount(): Int {
        return mediaItems.size
    }

    private fun startSelection(mediaFile: Media) {
        selectionCallback?.onStartSelection()
        isSelectionModeActive = true
        selectedItems.clear()
        toggleSelection(mediaFile)
    }

    private fun toggleSelection(mediaFile: Media) {
        val position = mediaItems.indexOf(mediaFile)
        if (selectedItems.contains(mediaFile)) {
            if (selectedItems.size == 1) {
                finishSelectionAndReset()
                onSelected?.invoke(mediaFile, false)
                return
            }
            selectedItems.remove(mediaFile)
            mediaFile.isSelected = false
        } else {
            selectedItems.add(mediaFile)
            mediaFile.isSelected = true
        }
        selectionCallback?.onUpdateSelection(selectedItems.size)
        onSelected?.invoke(mediaFile, selectedItems.contains(mediaFile))
        notifyItemChanged(position)
    }

    class AutoCompleteViewHolder(
        private val binding: ViewDataBinding,
        val context: Context,
        private val adapter: MediaItemAdapter,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindItem(data: Media) {
            binding.setVariable(BR.item, data)
            binding.executePendingBindings()

            when (binding) {
                is ItemImagesBinding -> bindImages(data)
                is ItemAudioBinding -> bindAudio(data)
                is ItemVideoBinding -> bindVideo(data)
                is ItemDocumentBinding -> bindDocument(data)
                is ItemDownloadsBinding -> bindDownload(data) // Add this line
            }

            binding.root.click {
                with(adapter) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        if (isSelectionModeActive) {
                            toggleSelection(mediaItems[position])
                        } else {
                            onClick?.invoke(mediaItems[position])
                        }
                    }
                }
            }

            binding.root.setOnLongClickListener {
                with(adapter) {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        if (!isSelectionModeActive) {
                            startSelection(mediaItems[position])
                        }
                    }
                }
                true
            }
        }

        // Todo FileNotFundEx -> media scanner or file ex control
        private fun bindImages(data: Media) {
            (binding as ItemImagesBinding).ivImage.loadWithGlide(data.uri)
            setSelectedVisibility(binding.ivSelected, binding.ivImage, data)
        }

        private fun bindAudio(data: Media) {
            Glide.with(binding.root).load(data.ext?.let { FileExtension.getIconResourceId(it) })
                .into((binding as ItemAudioBinding).ivImage)
            setSelectedVisibility(binding.ivSelected, binding.ivImage, data)
        }

        private fun bindVideo(data: Media) {
            (binding as ItemVideoBinding).ivImage.loadWithGlide(
                data.uri,
                R.drawable.layer_placeholder_transparent,
            ) { exception ->
                if (exception == null) {
                    binding.ivPlay.visible()
                } else {
                    binding.ivPlay.gone()
                }
            }
            setSelectedVisibility(binding.ivSelected, binding.ivImage, data)
        }

        private fun bindDocument(data: Media) {
            Glide.with(binding.root)
                .load(data.ext?.let { FileExtension.getIconResourceId(it) })
                .into((binding as ItemDocumentBinding).ivImage)
            setSelectedVisibility(binding.ivSelected, binding.ivImage, data)
        }

        private fun bindDownload(data: Media) {
            val iconResId = when {
                isAudio(data.ext) -> R.drawable.ic_audios_category // Replace with your audio icon drawable resource
                isImage(data.ext) -> R.drawable.ic_image_category // Replace with your audio icon drawable resource
                isVideo(data.ext) -> R.drawable.ic_videos_category // Replace with your video icon drawable resource
                isDocument(data.ext) -> R.drawable.ic_documents_category // Replace with your document icon drawable resource
                isApk(data.ext) -> R.drawable.ic_apk_category // Replace with your apk icon drawable resource
                else -> R.drawable.file_generic_icon // Replace with a generic file icon drawable resource
            }

            Glide.with(binding.root)
                .load(iconResId)
                .into((binding as ItemDownloadsBinding).ivImage)

            setSelectedVisibility(binding.ivSelected, binding.ivImage, data)
        }

        // Utility methods to determine the file type
        private fun isAudio(extension: String?): Boolean {
            // Example check, adjust based on your application's logic
            return listOf("mp3", "wav", "aac").contains(extension?.lowercase())
        }

        private fun isImage(extension: String?): Boolean {
            // Example check, adjust based on your application's logic
            return listOf("jpg", "jpeg", "png").contains(extension?.lowercase())
        }

        private fun isVideo(extension: String?): Boolean {
            return listOf("mp4", "mkv", "3gp").contains(extension?.lowercase())
        }

        private fun isDocument(extension: String?): Boolean {
            return listOf("pdf", "docx", "txt").contains(extension?.lowercase())
        }

        private fun isApk(extension: String?): Boolean {
            return "apk" == extension?.lowercase()
        }



        private fun setSelectedVisibility(
            ivSelected: AppCompatImageView,
            ivImage: AppCompatImageView,
            data: Media,
        ) {
            if (data.isSelected) {
                ivSelected.visible()
                ivImage.setMargins(50)
            } else {
                ivSelected.invisible()
                ivImage.setMargins(0)
            }
        }

        companion object {
            fun create(
                inflater: LayoutInflater?,
                parent: ViewGroup?,
                viewType: Int,
                adapter: MediaItemAdapter,
            ): AutoCompleteViewHolder {
                val binding =
                    DataBindingUtil.inflate<ViewDataBinding>(inflater!!, viewType, parent, false)
                return AutoCompleteViewHolder(
                    binding,
                    parent?.context!!,
                    adapter,
                )
            }
        }
    }

    interface SelectionCallback {
        fun onStartSelection()

        fun onUpdateSelection(selectedSize: Int)

        fun onEndSelection()
    }
}
