package dev.devtoolbox.ui

import androidx.compose.ui.input.pointer.PointerIcon
import java.awt.Cursor

actual val verticalResizeCursor: PointerIcon = PointerIcon(Cursor(Cursor.N_RESIZE_CURSOR))

actual val horizontalResizeCursor: PointerIcon = PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR))

actual val nwseResizeCursor: PointerIcon = PointerIcon(Cursor(Cursor.SE_RESIZE_CURSOR))

actual val neswResizeCursor: PointerIcon = PointerIcon(Cursor(Cursor.SW_RESIZE_CURSOR))
