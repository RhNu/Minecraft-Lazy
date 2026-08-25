package rhx.lazy.core.ui

import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import dev.vfyjxf.taffy.style.TaffyDimension

/**
 * Creates the common modal shell used by Lazy's secondary windows.
 *
 * LDLib2's [Dialog] owns a 150-pixel overlay plus a padded, painted content container. Lazy's
 * dialogs paint their own complete panel, so leaving that stock container visible produces a
 * second, narrower panel behind the real one. The stock container is deliberately transparent and
 * unpadded here while the overlay follows the custom content width.
 */
internal fun lazyModalDialog(
    content: UIElement,
    zIndex: Int = 2,
): Dialog =
    Dialog()
        .setAutoClose(false)
        .setClickOutsideClose(false)
        .darkenBackground()
        .apply {
            style { style -> style.zIndex(zIndex) }
            width(TaffyDimension.maxContent())
            titleBar.setDisplay(false)
            buttonContainer.setDisplay(false)
            contentContainer.layout { layout ->
                layout.paddingAll(0f)
                layout.gapAll(0f)
            }
            contentContainer.style { style -> style.backgroundTexture(ColorRectTexture(0)) }
            addContent(content)
            setDisplay(false)
            addEventListener("keyDown") { event ->
                if (event.keyCode == KEY_ESCAPE) {
                    closeLazyModal()
                    event.stopPropagation()
                }
            }
        }

internal fun Dialog.openLazyModal(focusTarget: UIElement? = null) {
    getModularUI()?.apply {
        shouldCloseOnEsc(false)
        shouldCloseOnKeyInventory(false)
    }
    setDisplay(true)
    focus()
    focusTarget?.focus()
}

internal fun Dialog.closeLazyModal() {
    setDisplay(false)
    getModularUI()?.apply {
        clearFocus()
        shouldCloseOnEsc(true)
        shouldCloseOnKeyInventory(true)
    }
}

private const val KEY_ESCAPE = 256
