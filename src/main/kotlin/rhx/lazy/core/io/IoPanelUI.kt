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
    val controller: IoRouteController?

    fun isValid(): Boolean
}

internal object IoPanelUI {
    val stylesheet = lazyId("lss/io.lss")

    fun addIoControl(
        parent: UIContainer<*, *>,
        model: IoPanelModel,
    ): (UIElement) -> Unit {
        val controller = model.controller
        val supportedRoutes = controller?.supportedRoutes ?: setOf(IoRoute.PASSIVE)
        val providers =
            NetworkOutputProviders
                .all()
                .filter { provider ->
                    IoRoute.NETWORK in supportedRoutes
                }

        val routeButtons = mutableMapOf<IoRoute, UIElement>()
        val providerButtons = mutableMapOf<net.minecraft.resources.ResourceLocation, UIElement>()
        val dialog =
            Dialog()
                .setAutoClose(true)
                .setClickOutsideClose(false)
                .darkenBackground()
                .apply {
                    style { dialogStyle -> dialogStyle.zIndex(1) }
                    width(TaffyDimension.maxContent())
                }
        val panel =
            element(
                {
                    cls = {
                        +"lazy-io__panel"
                    }
                },
            ) {
                column {
                    label(
                        {
                            text = Component.translatable("gui.lazy.io.title")
                            cls = { +"lazy-io__title" }
                        },
                    )
                    row(
                        {
                            cls = { +"lazy-io__routes" }
                        },
                    ) {
                        supportedRoutes
                            .filter { route -> route != IoRoute.NETWORK || providers.isNotEmpty() }
                            .forEach { route ->
                                routeButtons[route] =
                                    button(
                                        {
                                            noText()
                                            cls = { +"lazy-io__route-button" }
                                            style = { tooltips(routeTooltip(route)) }
                                            onServerClick = { event ->
                                                if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                                    if (route == IoRoute.NETWORK) {
                                                        providers.firstOrNull()?.let { selectNetwork(model, it.id) }
                                                    } else {
                                                        selectRoute(model, route)
                                                    }
                                                }
                                            }
                                        },
                                    ).element.apply {
                                        addPreIcon(ItemStackTexture(routeIcon(route)))
                                    }
                            }
                    }
                    if (providers.isNotEmpty()) {
                        row(
                            {
                                cls = { +"lazy-io__providers" }
                            },
                        ) {
                            providers.forEach { provider ->
                                providerButtons[provider.id] =
                                    button(
                                        {
                                            noText()
                                            cls = { +"lazy-io__provider-button" }
                                            style = { tooltips(providerTooltip(provider, controller)) }
                                            onServerClick = { event ->
                                                if (event.button == LEFT_MOUSE_BUTTON && model.isValid()) {
                                                    selectNetwork(model, provider.id)
                                                }
                                            }
                                        },
                                    ).element.apply {
                                        addPreIcon(ItemStackTexture(provider.icon()))
                                    }
                            }
                        }
                    }
                }
            }
        dialog.addContent(panel)
        dialog.setDisplay(false)

        parent
            .button(
                {
                    noText()
                    cls = { +"lazy-io__trigger" }
                    style = { tooltips(Component.translatable("gui.lazy.io.open")) }
                    onClick = { dialog.setDisplay(true) }
                },
            ).element
            .apply {
                addPreIcon(ItemStackTexture(ItemStack(Items.COMPARATOR)))
            }

        return { root ->
            root.addChild(dialog)
            routeButtons.forEach { (route, button) ->
                bindSelected(root, button) { model.controller?.route == route }
            }
            providerButtons.forEach { (providerId, button) ->
                bindSelected(root, button) { model.controller?.target?.providerId == providerId }
            }
        }
    }

    private fun bindSelected(
        root: UIElement,
        button: UIElement,
        selected: () -> Boolean,
    ) {
        val value = BindableValue(false)
        value.setDisplay(false)
        value.registerValueListener { isSelected ->
            if (isSelected) button.addClass(SELECTED_BUTTON_CLASS) else button.removeClass(SELECTED_BUTTON_CLASS)
        }
        value.bind(
            DataBindingBuilder
                .boolS2C { selected() }
                .initialValue(false)
                .build(),
        )
        root.addChild(value)
    }

    private fun selectRoute(
        model: IoPanelModel,
        route: IoRoute,
    ) {
        if (!model.isValid()) return
        model.controller?.setRoute(route)
    }

    private fun selectNetwork(
        model: IoPanelModel,
        providerId: net.minecraft.resources.ResourceLocation,
    ) {
        if (!model.isValid()) return
        val player = model.player as? ServerPlayer ?: return
        val provider = NetworkOutputProviders.get(providerId) ?: return
        when (val resolution = provider.resolvePrimaryTarget(player)) {
            is NetworkTargetResolution.Success -> {
                if (model.controller?.setNetworkTarget(resolution.target) == true) {
                    player.displayActionBar("message.lazy.io.network.success", provider.displayName)
                } else {
                    player.displayActionBar("message.lazy.io.network.incompatible")
                }
            }

            NetworkTargetResolution.Unavailable ->
                player.displayActionBar("message.lazy.io.network.unavailable")

            NetworkTargetResolution.NotFound ->
                player.displayActionBar("message.lazy.io.network.no_target")

            NetworkTargetResolution.Unlinked ->
                player.displayActionBar("message.lazy.io.network.unlinked")

            NetworkTargetResolution.Ambiguous ->
                player.displayActionBar("message.lazy.io.network.ambiguous")

            NetworkTargetResolution.Incompatible ->
                player.displayActionBar("message.lazy.io.network.incompatible")

            NetworkTargetResolution.Failed ->
                player.displayActionBar("message.lazy.io.network.failed")
        }
    }

    private fun routeTooltip(route: IoRoute): Component =
        Component.translatable(
            when (route) {
                IoRoute.PASSIVE -> "gui.lazy.io.route.passive"
                IoRoute.DOWNWARD -> "gui.lazy.io.route.downward"
                IoRoute.ADJACENT -> "gui.lazy.io.route.adjacent"
                IoRoute.NETWORK -> "gui.lazy.io.route.network"
            },
        )

    private fun providerTooltip(
        provider: NetworkOutputProvider,
        controller: IoRouteController?,
    ): Component =
        provider.displayName
            .copy()
            .append("\n")
            .append(
                Component.translatable(
                    "gui.lazy.io.provider.capabilities",
                    capabilityList(provider.capabilities),
                ),
            ).apply {
                if (controller?.capabilities?.none { it in provider.capabilities } == true) {
                    append("\n")
                    append(Component.translatable("gui.lazy.io.provider.incompatible"))
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

    private fun routeIcon(route: IoRoute): ItemStack =
        when (route) {
            IoRoute.PASSIVE -> ItemStack(Items.BARRIER)
            IoRoute.DOWNWARD -> ItemStack(Items.HOPPER)
            IoRoute.ADJACENT -> ItemStack(Items.DISPENSER)
            IoRoute.NETWORK -> ItemStack(Items.ENDER_CHEST)
        }

    private const val LEFT_MOUSE_BUTTON = 0
    private const val SELECTED_BUTTON_CLASS = "lazy-io__button--selected"
}
