
package com.sn.snfilemanager.core.util

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.sn.snfilemanager.R

class MaterialDialogUtils {

    data class DialogResult(val confirmed: Boolean, val text: String)
    data class DialogInfoResult(val confirmed: Boolean)

    @SuppressLint("InflateParams", "SuspiciousIndentation")
    fun createBasicMaterial(
        title: String, text: String, textPositiveButton: String, context: Context, callback: (DialogResult) -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.layout_basic_dialog, null)
        val eInputEditText = dialogView.findViewById<TextInputEditText>(R.id.eInputEditText)

        eInputEditText.setText(text)

        MaterialAlertDialogBuilder(context).setTitle(title).setView(dialogView).setCancelable(false)
            .setPositiveButton(textPositiveButton) { _, _ ->
                val enteredText = eInputEditText.text.toString()
                callback(DialogResult(true, enteredText))


            }.setNegativeButton(R.string.dialog_cancel) { _, _ ->
                callback(DialogResult(false, ""))
            }.show()
    }

    @SuppressLint("InflateParams", "SuspiciousIndentation")
    fun createDialogInfo(
        title: String,
        message: String,
        textPositiveButton: String,
        textNegativeButon: String = "null",
        context: Context,
        cancelable: Boolean,
        callback: (DialogInfoResult) -> Unit
    ) {


        val mDialog = MaterialAlertDialogBuilder(context).setTitle(title).setMessage(message)
            .setPositiveButton(textPositiveButton) { _, _ ->
                callback(DialogInfoResult(true))


            }
        if (cancelable) {
            mDialog.setCancelable(true)
            mDialog.setNegativeButton(textNegativeButon) { _, _ ->
                callback(DialogInfoResult(false))
            }
        }
        mDialog.show()
    }


}
