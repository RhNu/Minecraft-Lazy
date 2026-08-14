package rhx.lazy.core.io

import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.row
import dev.vfyjxf.taffy.style.TaffyDimension
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.lazyId

internal interface IoPanelModel {
    val player: Player
    val editor: IoConfigurationEditor?

    fun isValid(): Boolean
}

internal object IoPanelUI {
    val stylesheet = lazyId("lss/io.lss")

    fun addIoControl(
        parent: UIContainer<*, *>,
        model: IoPanelModel,
    ): (UIElement) -> Unit {
        lateinit var rootElement: UIElement
        val panel = createPanel(model)
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

        fun closeDialog() {
            dialog.setDisplay(false)
            rootElement.getModularUI()?.apply {
                clearFocus()
                shouldCloseOnEsc(true)
                shouldCloseOnKeyInventory(true)
            }
        }
        dialog.addEventListener("keyDown") { event ->
            if (event.keyCode == KEY_E || event.keyCode == KEY_ESCAPE) {
                closeDialog()
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
                        rootElement.getModularUI()?.apply {
                            shouldCloseOnEsc(false)
                            shouldCloseOnKeyInventory(false)
                        }
                        dialog.setDisplay(true)
                        dialog.focus()
                    }
                },
            ).element
            .apply { addPreIcon(ItemStackTexture(ItemStack(Items.COMPARATOR))) }

        return { root ->
            rootElement = root
            root.addChild(dialog)
            panel.install(root)
        }
    }

    fun addIoPanel(
        parent: UIContainer<*, *>,
        model: IoPanelModel,
    ): (UIElement) -> Unit {
        val panel = createPanel(model)
        parent.element.addChild(panel.element)
        return panel.install
    }

    private fun createPanel(model: IoPanelModel): Panel {
        val tabButtons = mutableMapOf<IoMode, UIElement>()
        val contentElements = mutableMapOf<IoMode, UIElement>()
        val sideButtons = mutableMapOf<RelativeSide, UIElement>()
        val providerButtons = mutableMapOf<net.minecraft.resources.ResourceLocation, UIElement>()
        lateinit var ejectButton: UIElement
        val providers = NetworkOutputProviders.all()

        val panelRoot =
            element(
                {
                    cls = { +"lazy-io__panel" }
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
                        tabButtons[mode] =
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
                contentElements[IoMode.PASSIVE] =
                    element(
                        {
                            cls = { +"lazy-io__content" }
                            style = { tooltips(Component.translatable("gui.lazy.io.passive_hint")) }
                        },
                    ) {
                        button(
                            {
                                active = false
                                noText()
                                cls = { +"lazy-io__passive-icon" }
                            },
                        ).element.apply { addPreIcon(ItemStackTexture(ItemStack(Items.CHEST))) }
                    }.element
                contentElements[IoMode.FACE] =
                    element(
                        {
                            cls = { +"lazy-io__content" }
                        },
                    ) {
                        column(
                            {
                                cls = { +"lazy-io__face-grid" }
                            },
                        ) {
                            sideRow(model, sideButtons, null, RelativeSide.TOP, null)
                            sideRow(model, sideButtons, RelativeSide.LEFT, RelativeSide.FRONT, RelativeSide.RIGHT)
                            sideRow(model, sideButtons, null, RelativeSide.BOTTOM, RelativeSide.BACK)
                        }
                        ejectButton =
                            button(
                                {
                                    text = Component.translatable("gui.lazy.io.auto_eject")
                                    cls = { +"lazy-io__eject" }
                                    style = { tooltips(Component.translatable("gui.lazy.io.auto_eject.tooltip")) }
                                    onServerClick = { event ->
                                        if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                            model.editor?.toggleAutoEject()
                                        }
                                    }
                                },
                            ).element
                    }.element
                contentElements[IoMode.NETWORK] =
                    element(
                        {
                            cls = { +"lazy-io__content" }
                        },
                    ) {
                        column(
                            {
                                cls = { +"lazy-io__network-list" }
                            },
                        ) {
                            if (providers.isEmpty()) {
                                label(
                                    {
                                        text = Component.translatable("gui.lazy.io.network.empty")
                                        cls = { +"lazy-io__network-empty" }
                                    },
                                )
                            }
                            providers.forEach { provider ->
                                providerButtons[provider.id] =
                                    button(
                                        {
                                            text = provider.displayName
                                            cls = { +"lazy-io__provider-button" }
                                            onServerClick = { event ->
                                                if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                                    selectNetwork(model, provider)
                                                }
                                            }
                                        },
                                    ).element.apply { addPreIcon(ItemStackTexture(provider.icon())) }
                            }
                        }
                    }.element
            }

        val install: (UIElement) -> Unit = { root ->
            tabButtons.forEach { (mode, button) ->
                bindSelected(root, button) { model.editor?.configuration?.mode == mode }
            }
            contentElements.forEach { (mode, content) ->
                bindDisplay(root, content) { model.editor?.configuration?.mode == mode }
            }
            sideButtons.forEach { (side, button) ->
                bindSideMode(root, button, side) { model.editor?.configuration?.side(side) ?: SideIoMode.NONE }
            }
            bindSelected(root, ejectButton) { model.editor?.configuration?.autoEject == true }
            providerButtons.forEach { (providerId, button) ->
                bindSelected(root, button) {
                    val configuration = model.editor?.configuration
                    configuration?.mode == IoMode.NETWORK && configuration.networkTarget?.providerId == providerId
                }
                NetworkOutputProviders.get(providerId)?.let { provider ->
                    bindTooltip(root, button) { providerTooltip(provider, model.editor) }
                }
            }
        }
        return Panel(panelRoot, install)
    }

    private fun UIContainer<*, *>.sideRow(
        model: IoPanelModel,
        buttons: MutableMap<RelativeSide, UIElement>,
        first: RelativeSide?,
        second: RelativeSide?,
        third: RelativeSide?,
    ) {
        row(
            {
                cls = { +"lazy-io__face-row" }
            },
        ) {
            listOf(first, second, third).forEach { side ->
                if (side == null) {
                    element({ cls = { +"lazy-io__face-placeholder" } })
                } else {
                    buttons[side] =
                        button(
                            {
                                text = Component.translatable("gui.lazy.io.side.${side.name.lowercase()}.short")
                                cls = { +"lazy-io__face-button" }
                                style = { tooltips(Component.translatable("gui.lazy.io.side.${side.name.lowercase()}")) }
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

    private fun bindSelected(
        root: UIElement,
        element: UIElement,
        selected: () -> Boolean,
    ) {
        val value = BindableValue(false)
        value.setDisplay(false)
        value.registerValueListener { isSelected ->
            if (isSelected) element.addClass(SELECTED_CLASS) else element.removeClass(SELECTED_CLASS)
        }
        value.bind(DataBindingBuilder.boolS2C(selected).initialValue(false).build())
        root.addChild(value)
    }

    private fun bindDisplay(
        root: UIElement,
        element: UIElement,
        displayed: () -> Boolean,
    ) {
        val value = BindableValue(false)
        value.setDisplay(false)
        value.registerValueListener(element::setDisplay)
        value.bind(DataBindingBuilder.boolS2C(displayed).initialValue(false).build())
        root.addChild(value)
    }

    private fun bindSideMode(
        root: UIElement,
        element: UIElement,
        side: RelativeSide,
        sideMode: () -> SideIoMode,
    ) {
        val value = BindableValue(SideIoMode.NONE)
        value.setDisplay(false)
        value.registerValueListener { mode ->
            SideIoMode.entries.forEach { element.removeClass("lazy-io__face--${it.name.lowercase()}") }
            element.addClass("lazy-io__face--${mode.name.lowercase()}")
            element.style { style ->
                style.tooltips(
                    Component
                        .translatable("gui.lazy.io.side.${side.name.lowercase()}")
                        .append("\n")
                        .append(Component.translatable("gui.lazy.io.side_mode.${mode.name.lowercase()}")),
                )
            }
        }
        value.bind(DataBindingBuilder.enumValS2C(SideIoMode::class.java, sideMode).initialValue(SideIoMode.NONE).build())
        root.addChild(value)
    }

    private fun bindTooltip(
        root: UIElement,
        element: UIElement,
        tooltip: () -> Component,
    ) {
        val value = BindableValue<Component>(Component.empty())
        value.setDisplay(false)
        value.registerValueListener { component -> element.style { style -> style.tooltips(component) } }
        value.bind(DataBindingBuilder.componentS2C(tooltip).build())
        root.addChild(value)
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

    private fun providerTooltip(
        provider: NetworkOutputProvider,
        editor: IoConfigurationEditor?,
    ): Component =
        provider.displayName
            .copy()
            .append("\n")
            .append(Component.translatable("gui.lazy.io.provider.capabilities", capabilityList(provider.capabilities)))
            .apply {
                if (editor?.capabilities?.none { it in provider.capabilities } == true) {
                    append("\n")
                    append(Component.translatable("gui.lazy.io.provider.incompatible"))
                }
                if (editor?.networkPaused == true) {
                    append("\n")
                    append(Component.translatable("gui.lazy.io.network_paused"))
                }
            }

    private fun capabilityList(capabilities: Set<NetworkInsertCapability>): Component {
        val result = Component.empty()
        capabilities.forEachIndexed { index, capability ->
            if (index > 0) result.append(", ")
            result.append(capability.displayName)
        }
        return result
    }

    private data class Panel(
        val element: UIElement,
        val install: (UIElement) -> Unit,
    )

    private const val LEFT_MOUSE_BUTTON = 0
    private const val KEY_E = 69
    private const val KEY_ESCAPE = 256
    private const val SELECTED_CLASS = "lazy-io__button--selected"
}
