package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import dev.vfyjxf.taffy.style.TaffyDimension
import dev.vfyjxf.taffy.style.TaffyPosition
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.lazyId
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.api.LazyInternalApi

@LazyInternalApi
public interface IoPanelModel {
    val player: Player
    val editor: IoConfigurationEditor?

    fun isValid(): Boolean
}

/**
 * The IO configuration panel shared by every machine and by the configuration card.
 *
 * A client never holds a machine's IO configuration, so everything the panel draws arrives through
 * server-to-client bindings carrying the codes in [IoPanelState]. The three tabs are the three
 * modes, and the body is fixed to the height of the tallest one so picking a tab changes what the
 * window shows and never how large it is.
 */
@LazyInternalApi
public object IoPanelUI {
    val stylesheet = lazyId("lss/io.lss")

    /** Adds a toolbar button that opens the panel as a modal dialog over an existing screen. */
    fun addIoControl(
        parent: UIContainer<*, *>,
        model: IoPanelModel,
    ): (UIElement) -> Unit {
        val panel = createPanel(model, standalone = false)
        val dialog =
            Dialog()
                .setAutoClose(false)
                .setClickOutsideClose(false)
                .darkenBackground()
                .apply {
                    style { dialogStyle -> dialogStyle.zIndex(1) }
                    width(TaffyDimension.maxContent())
                }
        dialog.titleBar.setDisplay(false)
        dialog.buttonContainer.setDisplay(false)
        dialog.addContent(panel.element)
        dialog.setDisplay(false)

        fun setModalCapture(active: Boolean) {
            dialog.getModularUI()?.apply {
                if (!active) clearFocus()
                shouldCloseOnEsc(!active)
                shouldCloseOnKeyInventory(!active)
            }
        }
        dialog.addEventListener("keyDown") { event ->
            if (event.keyCode == KEY_E || event.keyCode == KEY_ESCAPE) {
                dialog.setDisplay(false)
                setModalCapture(false)
                event.stopPropagation()
            }
        }

        parent
            .button(
                {
                    noText()
                    cls = { +"lazy-io__trigger" }
                    style = { tooltips(Component.translatable("gui.lazy.io.open")) }
                    onClick = {
                        setModalCapture(true)
                        dialog.setDisplay(true)
                        dialog.focus()
                    }
                },
            ).element
            .apply { addPreIcon(ItemStackTexture(ItemStack(Items.COMPARATOR))) }

        return { root ->
            root.addChild(dialog)
            panel.install(root)
        }
    }

    /** Builds a whole screen out of the panel, for holders that configure IO and nothing else. */
    fun createStandaloneUI(model: IoPanelModel): ModularUI {
        val panel = createPanel(model, standalone = true)
        panel.install(panel.element)
        return ModularUI(UI.of(panel.element, StylesheetManager.MC, stylesheet), model.player)
    }

    private fun createPanel(
        model: IoPanelModel,
        standalone: Boolean,
    ): Panel {
        val view = PanelView(NetworkOutputProviders.all())
        val root =
            element(
                {
                    cls = {
                        if (standalone) {
                            +"panel_bg"
                        }
                        +"lazy-io__panel"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("gui.lazy.io.title")
                        cls = { +"lazy-io__title" }
                    },
                )
                row(
                    {
                        cls = { +"lazy-io__tabs" }
                    },
                ) {
                    IoMode.entries.forEach { mode ->
                        view.tabs[mode] =
                            button(
                                {
                                    text = mode.translation()
                                    cls = { +"lazy-io__tab" }
                                    onServerClick = { event ->
                                        if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                            model.editor?.setMode(mode)
                                        }
                                    }
                                },
                            ).element
                    }
                }
                element(
                    {
                        cls = { +"lazy-io__body" }
                        layout = { height(bodyHeight(view.providers.size)) }
                    },
                ) {
                    view.contents[IoMode.PASSIVE] = passiveContent()
                    view.contents[IoMode.FACE] = faceContent(model, view)
                    view.contents[IoMode.NETWORK] = networkContent(model, view)
                }
            }
        view.reset()
        return Panel(root) { host -> view.bind(host, model) }
    }
}

private class Panel(
    val element: UIElement,
    val install: (UIElement) -> Unit,
)

// ---------------------------------------------------------------------------
// Tab contents
// ---------------------------------------------------------------------------

private fun UIContainer<*, *>.passiveContent(): UIElement =
    element(
        {
            cls = { +"lazy-io__content" }
        },
    ) {
        button(
            {
                active = false
                noText()
                cls = { +"lazy-io__passive-icon" }
            },
        ).element.apply { addPreIcon(ItemStackTexture(ItemStack(Items.CHEST))) }
        label(
            {
                text = Component.translatable("gui.lazy.io.passive_hint")
                cls = { +"lazy-io__hint" }
            },
        )
    }.element

private fun UIContainer<*, *>.faceContent(
    model: IoPanelModel,
    view: PanelView,
): UIElement =
    element(
        {
            cls = { +"lazy-io__content" }
        },
    ) {
        // An unfolded cube around the front face, so every button sits where that side really is.
        column(
            {
                cls = { +"lazy-io__face-grid" }
            },
        ) {
            faceRow(model, view, null, RelativeSide.TOP, RelativeSide.BACK)
            faceRow(model, view, RelativeSide.LEFT, RelativeSide.FRONT, RelativeSide.RIGHT)
            faceRow(model, view, null, RelativeSide.BOTTOM, null)
        }
        // Pinned to the corner so the grid stays centred no matter how wide the toggle is.
        view.eject =
            button(
                {
                    noText()
                    cls = { +"lazy-io__eject" }
                    layout = {
                        position(TaffyPosition.ABSOLUTE)
                        pos {
                            top(0)
                            right(0)
                        }
                    }
                    onServerClick = { event ->
                        if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                            model.editor?.toggleAutoEject()
                        }
                    }
                },
            ).element.apply { addPreIcon(ItemStackTexture(ItemStack(Items.HOPPER))) }
    }.element

private fun UIContainer<*, *>.faceRow(
    model: IoPanelModel,
    view: PanelView,
    left: RelativeSide?,
    center: RelativeSide?,
    right: RelativeSide?,
) {
    row(
        {
            cls = { +"lazy-io__face-row" }
        },
    ) {
        listOf(left, center, right).forEach { side ->
            if (side == null) {
                element({ cls = { +"lazy-io__face-placeholder" } })
            } else {
                view.sides[side] =
                    button(
                        {
                            text = Component.translatable("gui.lazy.io.side.${side.key}.short")
                            cls = { +"lazy-io__face" }
                            onServerClick = { event ->
                                if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                    model.editor?.cycleSide(side)
                                }
                            }
                        },
                    ).element
            }
        }
    }
}

private fun UIContainer<*, *>.networkContent(
    model: IoPanelModel,
    view: PanelView,
): UIElement =
    element(
        {
            cls = { +"lazy-io__content" }
        },
    ) {
        view.status =
            label(
                {
                    text = Component.empty()
                    cls = { +"lazy-io__network-status" }
                },
            ).element
        column(
            {
                cls = { +"lazy-io__network-list" }
            },
        ) {
            if (view.providers.isEmpty()) {
                label(
                    {
                        text = Component.translatable("gui.lazy.io.network.empty")
                        cls = { +"lazy-io__network-empty" }
                    },
                )
            }
            view.providers.forEach { provider ->
                view.providerButtons +=
                    button(
                        {
                            text = provider.displayName
                            cls = { +"lazy-io__provider" }
                            onServerClick = { event ->
                                if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                    selectNetwork(model, provider)
                                }
                            }
                        },
                    ).element.apply { addPreIcon(ItemStackTexture(provider.icon())) }
            }
        }
        row(
            {
                cls = { +"lazy-io__network-actions" }
            },
        ) {
            // Both actions stay in place and grey out instead of disappearing, so the row never
            // reflows; an inactive button still reaches the server, hence the guards below.
            view.resume =
                button(
                    {
                        text = Component.translatable("gui.lazy.io.network.resume")
                        cls = { +"lazy-io__network-action" }
                        style = { tooltips(Component.translatable("gui.lazy.io.network.resume.tooltip")) }
                        onServerClick = { event ->
                            if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                model.editor?.takeIf { it.networkPaused }?.resumeNetwork()
                            }
                        }
                    },
                ).element
            view.disconnect =
                button(
                    {
                        text = Component.translatable("gui.lazy.io.network.disconnect")
                        cls = { +"lazy-io__network-action" }
                        style = { tooltips(Component.translatable("gui.lazy.io.network.disconnect.tooltip")) }
                        onServerClick = { event ->
                            if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                model.editor?.takeIf { it.configuration.networkTarget != null }?.clearNetworkTarget()
                            }
                        }
                    },
                ).element
        }
    }.element

// ---------------------------------------------------------------------------
// Synced state
// ---------------------------------------------------------------------------

/**
 * The widgets a synced update has to touch, plus the code that repaints them.
 *
 * Every `apply` method is total: it assigns an appearance to each widget of its group rather than
 * only to the ones that changed, which is what keeps a switched-away tab from lingering.
 */
private class PanelView(
    val providers: List<NetworkOutputProvider>,
) {
    val tabs = mutableMapOf<IoMode, UIElement>()
    val contents = mutableMapOf<IoMode, UIElement>()
    val sides = mutableMapOf<RelativeSide, UIElement>()
    val providerButtons = mutableListOf<UIElement>()
    lateinit var eject: UIElement
    lateinit var status: Label
    lateinit var resume: UIElement
    lateinit var disconnect: UIElement

    /** Appearance of a freshly built panel; the first sync arrives before the first frame. */
    fun reset() {
        applyMode(IoPanelState.encodeMode(null))
        applySides(IoPanelState.encodeSides(null))
        applyAutoEject(IoPanelState.encodeAutoEject(null))
        applyNetwork(IoPanelState.NO_NETWORK)
        applyCompatibility(0)
    }

    fun bind(
        host: UIElement,
        model: IoPanelModel,
    ) {
        host.bindInt({ IoPanelState.encodeMode(model.editor) }, ::applyMode)
        host.bindInt({ IoPanelState.encodeSides(model.editor?.configuration) }, ::applySides)
        host.bindInt({ IoPanelState.encodeAutoEject(model.editor) }, ::applyAutoEject)
        host.bindInt({ IoPanelState.encodeNetwork(model.editor, providers) }, ::applyNetwork)
        host.bindInt({ IoPanelState.encodeCompatibility(model.editor, providers) }, ::applyCompatibility)
    }

    fun applyMode(code: Int) {
        val mode = IoPanelState.decodeMode(code)
        tabs.forEach { (tabMode, tab) -> tab.setSelected(tabMode == mode) }
        contents.forEach { (contentMode, content) -> content.setDisplay(contentMode == mode) }
    }

    fun applySides(code: Int) {
        sides.forEach { (side, button) ->
            val sideMode = IoPanelState.decodeSide(code, side)
            SideIoMode.entries.forEach { button.removeClass(it.faceClass) }
            button.addClass(sideMode.faceClass)
            button.setTooltip(
                Component.translatable("gui.lazy.io.side.${side.key}"),
                Component.translatable("gui.lazy.io.side_mode.${sideMode.name.lowercase()}"),
            )
        }
    }

    fun applyAutoEject(code: Int) {
        val enabled = IoPanelState.decodeAutoEject(code)
        eject.setSelected(enabled)
        eject.setTooltip(
            Component.translatable("gui.lazy.io.auto_eject"),
            Component.translatable(if (enabled) "gui.lazy.io.enabled" else "gui.lazy.io.disabled"),
            Component.translatable("gui.lazy.io.auto_eject.tooltip"),
        )
    }

    fun applyNetwork(code: Int) {
        val paused = IoPanelState.decodeNetworkPaused(code)
        val slot = IoPanelState.decodeNetworkSlot(code)
        providerButtons.forEachIndexed { index, button -> button.setSelected(slot == index + 1) }
        status.setText(networkStatus(paused, slot, providers))
        resume.setActive(paused)
        disconnect.setActive(slot != IoPanelState.NO_NETWORK)
    }

    fun applyCompatibility(mask: Int) {
        providerButtons.forEachIndexed { index, button ->
            val provider = providers.getOrNull(index) ?: return@forEachIndexed
            button.setTooltip(*providerTooltip(provider, IoPanelState.decodeCompatible(mask, index)))
        }
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun UIElement.bindInt(
    value: () -> Int,
    apply: (Int) -> Unit,
) {
    val bindable = BindableValue(IoPanelState.UNSYNCED)
    bindable.setDisplay(false)
    bindable.registerValueListener { code -> if (code != IoPanelState.UNSYNCED) apply(code) }
    bindable.bind(DataBindingBuilder.intValS2C(value).initialValue(IoPanelState.UNSYNCED).build())
    addChild(bindable)
}

private fun UIElement.setSelected(selected: Boolean) {
    if (selected) addClass(SELECTED_CLASS) else removeClass(SELECTED_CLASS)
}

private fun UIElement.setTooltip(vararg lines: Component) {
    style { style -> style.tooltips(*lines) }
}

private fun selectNetwork(
    model: IoPanelModel,
    provider: NetworkOutputProvider,
) {
    val player = model.player as? ServerPlayer ?: return
    when (val resolution = provider.resolvePrimaryTarget(player)) {
        is NetworkTargetResolution.Success -> {
            if (model.editor?.setNetworkTarget(resolution.target) == true) {
                player.displayActionBar("message.lazy.io.network.success", provider.displayName)
            } else {
                player.displayActionBar("message.lazy.io.network.incompatible")
            }
        }
        NetworkTargetResolution.Unavailable -> player.displayActionBar("message.lazy.io.network.unavailable")
        NetworkTargetResolution.NotFound -> player.displayActionBar("message.lazy.io.network.no_target")
        NetworkTargetResolution.Unlinked -> player.displayActionBar("message.lazy.io.network.unlinked")
        NetworkTargetResolution.Ambiguous -> player.displayActionBar("message.lazy.io.network.ambiguous")
        NetworkTargetResolution.Incompatible -> player.displayActionBar("message.lazy.io.network.incompatible")
        NetworkTargetResolution.Failed -> player.displayActionBar("message.lazy.io.network.failed")
    }
}

private fun networkStatus(
    paused: Boolean,
    slot: Int,
    providers: List<NetworkOutputProvider>,
): Component =
    when {
        paused -> Component.translatable("gui.lazy.io.network_paused")
        slot == IoPanelState.NO_NETWORK -> Component.translatable("gui.lazy.io.network.unbound")
        else ->
            Component.translatable(
                "gui.lazy.io.network.bound",
                providers.getOrNull(slot - 1)?.displayName ?: Component.translatable("gui.lazy.io.network.unknown"),
            )
    }

private fun providerTooltip(
    provider: NetworkOutputProvider,
    compatible: Boolean,
): Array<Component> =
    buildList {
        add(provider.displayName)
        add(Component.translatable("gui.lazy.io.provider.capabilities", capabilityList(provider.capabilities)))
        if (!compatible) add(Component.translatable("gui.lazy.io.provider.incompatible"))
    }.toTypedArray()

private fun capabilityList(capabilities: Set<ResourceKind<out ResourceVariant>>): Component {
    val result = Component.empty()
    capabilities.forEachIndexed { index, capability ->
        if (index > 0) result.append(", ")
        result.append(capability.displayName)
    }
    return result
}

/**
 * Height of the box that holds the tab contents, fixed to the tallest tab so switching tabs never
 * resizes the window. The terms mirror the matching heights and gaps in `lss/io.lss`; only the
 * network list varies, and only with the number of registered providers.
 */
private fun bodyHeight(providerCount: Int): Int {
    val listHeight =
        if (providerCount == 0) {
            NETWORK_EMPTY_HEIGHT
        } else {
            providerCount * PROVIDER_HEIGHT + (providerCount - 1) * PROVIDER_GAP
        }
    val passive = PASSIVE_ICON_SIZE + CONTENT_GAP + HINT_HEIGHT
    val face = FACE_ROW_COUNT * FACE_ROW_HEIGHT + (FACE_ROW_COUNT - 1) * FACE_ROW_GAP
    val network = NETWORK_STATUS_HEIGHT + CONTENT_GAP + listHeight + CONTENT_GAP + NETWORK_ACTION_HEIGHT
    return maxOf(passive, face, network) + CONTENT_PADDING * 2
}

private val RelativeSide.key: String
    get() = name.lowercase()

private val SideIoMode.faceClass: String
    get() = "lazy-io__face--${name.lowercase()}"

private const val LEFT_MOUSE_BUTTON = 0
private const val KEY_E = 69
private const val KEY_ESCAPE = 256
private const val SELECTED_CLASS = "lazy-io__button--selected"

private const val CONTENT_PADDING = 4
private const val CONTENT_GAP = 4
private const val PASSIVE_ICON_SIZE = 24
private const val HINT_HEIGHT = 20
private const val FACE_ROW_COUNT = 3
private const val FACE_ROW_HEIGHT = 18
private const val FACE_ROW_GAP = 2
private const val NETWORK_STATUS_HEIGHT = 14
private const val NETWORK_ACTION_HEIGHT = 16
private const val NETWORK_EMPTY_HEIGHT = 10
private const val PROVIDER_HEIGHT = 18
private const val PROVIDER_GAP = 2
