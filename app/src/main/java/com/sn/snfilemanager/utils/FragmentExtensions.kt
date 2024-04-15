package com.sn.snfilemanager.utils


import androidx.annotation.ArrayRes
import androidx.annotation.PluralsRes
import androidx.fragment.app.Fragment
import com.sn.snfilemanager.utils.getQuantityString
import com.sn.snfilemanager.utils.getStringArray


fun Fragment.getQuantityString(@PluralsRes id: Int, quantity: Int): String =
    requireContext().getQuantityString(id, quantity)

fun Fragment.getQuantityString(
    @PluralsRes id: Int, quantity: Int, vararg formatArgs: Any?
): String = requireContext().getQuantityString(id, quantity, *formatArgs)


fun Fragment.getStringArray(@ArrayRes id: Int) = requireContext().getStringArray(id)


