package rhx.lazy.feature.teleporter

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIBuilder
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.ScrollerView
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.textField
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.lazyId
import rhx.lazy.feature.voidworld.EncapsulatedSpace
import rhx.lazy.feature.voidworld.EncapsulatedSpaceResult
import rhx.lazy.feature.voidworld.EncapsulatedSpaceService
import rhx.lazy.integration.api.LazyInternalApi
import java.util.UUID

@LazyInternalApi
public object TeleporterUI {
    private val uiId = lazyId("teleporter")
    private val stylesheet = lazyId("lss/teleporter.lss")

    internal fun register() {
        PlayerUIMenuType.register(uiId) { _ ->
            PlayerUIMenuType.PlayerUIHolder { player -> create(player) }
        }
    }

    public fun open(player: ServerPlayer): Boolean {
        if (TeleporterActivation.isDimensionBlacklisted(player)) {
            player.displayActionBar(TeleporterActivation.DIMENSION_BLACKLISTED)
            return false
        }
        return PlayerUIMenuType.openUI(player, uiId)
    }

    private fun create(player: Player): ModularUI {
        val model = Model(player)
        val hiddenBindings = ArrayList<UIElement>()
        val scroller = ScrollerView().apply { addClass("lazy-teleporter__list") }

        repeat(PAGE_SIZE) { rowIndex ->
            val rowButton = Button()
            rowButton.noText()
            rowButton.addClass("lazy-teleporter__space-row")
            rowButton.addChild(boundLabel({ model.rowLabel(rowIndex) }, "lazy-teleporter__space-label"))
            rowButton.setOnServerClick { event ->
                if (event.button == LEFT_MOUSE_BUTTON) model.selectRow(rowIndex)
            }
            hiddenBindings += visibleBinding({ model.rowVisible(rowIndex) }, rowButton)
            scroller.addScrollViewChild(rowButton)
        }

        lateinit var renameField: TextField
        lateinit var deleteDialog: Dialog
        val uuidLabel = boundLabel(model::selectedUuidLabel, "lazy-teleporter__uuid")
        hiddenBindings += tooltipBinding(model::selectedUuidTooltip, uuidLabel)

        val root =
            element(
                {
                    cls = {
                        +"panel_bg"
                        +"lazy-teleporter"
                    }
                },
            ) {
                label(
                    {
                        text = Component.translatable("item.lazy.teleporter")
                        cls = { +"lazy-teleporter__title" }
                    },
                )
                row({ cls = { +"lazy-teleporter__body" } }) {
                    column({ cls = { +"lazy-teleporter__left" } }) {
                        row({ cls = { +"lazy-teleporter__list-actions" } }) {
                            actionButton("gui.lazy.teleporter.create") { model.createSpace() }
                            actionButton("<") { model.previousPage() }
                            addChild(raw(boundLabel(model::pageLabel, "lazy-teleporter__page")))
                            actionButton(">") { model.nextPage() }
                        }
                        addChild(raw(scroller))
                    }
                    column({ cls = { +"lazy-teleporter__details" } }) {
                        addChild(raw(boundLabel(model::selectedName, "lazy-teleporter__selected-name")))
                        addChild(raw(uuidLabel))
                        addChild(raw(boundLabel(model::selectedOwner, "lazy-teleporter__owner")))
                        renameField =
                            textField(
                                {
                                    text = ""
                                    cls = { +"lazy-teleporter__rename" }
                                },
                            ).element.apply {
                                setAnyString()
                                bind(
                                    DataBindingBuilder
                                        .string(model::renameText, model::setRenameText)
                                        .initialValue("")
                                        .build(),
                                )
                            }
                        actionButton("gui.lazy.teleporter.rename") { model.renameSelected() }
                        actionButton("gui.lazy.teleporter.travel") { model.teleportSelected() }
                        actionButton("gui.lazy.teleporter.hub") { model.teleportHub() }
                        actionButton("gui.lazy.teleporter.return") { model.returnOutside() }
                        button(
                            {
                                text = Component.translatable("gui.lazy.teleporter.delete")
                                cls = {
                                    +"lazy-teleporter__action"
                                    +"lazy-teleporter__delete"
                                }
                                onClick = {
                                    deleteDialog.setDisplay(true)
                                    deleteDialog.focus()
                                }
                            },
                        )
                    }
                }
            }

        deleteDialog = createDeleteDialog(model)
        hiddenBindings.forEach(root::addChild)
        root.addChild(deleteDialog)
        return ModularUI(UI.of(root, StylesheetManager.MC, stylesheet), player)
    }

    private fun createDeleteDialog(model: Model): Dialog {
        lateinit var dialog: Dialog
        val content =
            element({ cls = { +"lazy-teleporter__confirm" } }) {
                addChild(raw(boundLabel(model::deletePrompt, "lazy-teleporter__confirm-text")))
                row({ cls = { +"lazy-teleporter__confirm-actions" } }) {
                    button(
                        {
                            text = Component.translatable("gui.cancel")
                            cls = { +"lazy-teleporter__confirm-button" }
                            onClick = { dialog.setDisplay(false) }
                        },
                    )
                    button(
                        {
                            text = Component.translatable("gui.lazy.teleporter.confirm_delete")
                            cls = {
                                +"lazy-teleporter__confirm-button"
                                +"lazy-teleporter__delete"
                            }
                            onClick = { dialog.setDisplay(false) }
                            onServerClick = { event ->
                                if (event.button == LEFT_MOUSE_BUTTON) model.deleteSelected()
                            }
                        },
                    )
                }
            }
        dialog =
            Dialog()
                .setAutoClose(false)
                .setClickOutsideClose(false)
                .darkenBackground()
                .apply {
                    titleBar.setDisplay(false)
                    buttonContainer.setDisplay(false)
                    addContent(content)
                    setDisplay(false)
                }
        return dialog
    }

    private fun com.lowdragmc.lowdraglib2.gui.ui.UIContainer<*, *>.actionButton(
        translationKey: String,
        action: () -> Unit,
    ) {
        button(
            {
                text = if (translationKey.length == 1) Component.literal(translationKey) else Component.translatable(translationKey)
                cls = { +"lazy-teleporter__action" }
                onServerClick = { event ->
                    if (event.button == LEFT_MOUSE_BUTTON) action()
                }
            },
        )
    }

    private fun boundLabel(
        supplier: () -> Component,
        cssClass: String,
    ): Label =
        Label().apply {
            setValue(Component.empty())
            addClass(cssClass)
            bind(DataBindingBuilder.componentS2C(supplier).initialValue(Component.empty()).build())
        }

    private fun visibleBinding(
        supplier: () -> Boolean,
        target: UIElement,
    ): BindableValue<Boolean> =
        BindableValue(false).apply {
            setDisplay(false)
            registerValueListener(target::setVisible)
            bind(DataBindingBuilder.boolS2C(supplier).initialValue(false).build())
        }

    private fun tooltipBinding(
        supplier: () -> Component,
        target: UIElement,
    ): BindableValue<Component> =
        BindableValue<Component>(Component.empty()).apply {
            setDisplay(false)
            registerValueListener { tooltip -> target.style { style -> style.tooltips(tooltip) } }
            bind(DataBindingBuilder.componentS2C(supplier).initialValue(Component.empty()).build())
        }

    private class Model(
        private val player: Player,
    ) {
        private var selectedId: UUID? = null
        private var page = 0
        private var rename = ""
        private var cacheTick = Int.MIN_VALUE
        private var cachedSpaces: List<EncapsulatedSpace> = emptyList()

        init {
            serverPlayer()?.let { serverPlayer ->
                val spaces = spaces()
                val preferred = serverPlayer.getData(TeleporterRegistries.playerState.get()).selectedSpaceId
                selectedId = spaces.firstOrNull { space -> space.id == preferred }?.id ?: spaces.firstOrNull()?.id
                rename = selectedSpace()?.customName.orEmpty()
            }
        }

        fun rowLabel(rowIndex: Int): Component {
            val space = rowSpace(rowIndex) ?: return Component.empty()
            return Component
                .empty()
                .append(space.displayName())
                .append(Component.literal(" · ${space.shortId}").withStyle(ChatFormatting.DARK_GRAY))
        }

        fun rowVisible(rowIndex: Int): Boolean = rowSpace(rowIndex) != null

        fun selectRow(rowIndex: Int) {
            val space = rowSpace(rowIndex) ?: return
            selectedId = space.id
            rename = space.customName.orEmpty()
        }

        fun createSpace() {
            val serverPlayer = serverPlayer() ?: return
            when (val result = EncapsulatedSpaceService.create(serverPlayer)) {
                is EncapsulatedSpaceResult.Success -> {
                    invalidate()
                    result.space?.let { space ->
                        selectedId = space.id
                        rename = ""
                        page = 0
                    }
                }
                is EncapsulatedSpaceResult.Failure -> serverPlayer.displayActionBar(result.translationKey)
            }
        }

        fun renameSelected() {
            val serverPlayer = serverPlayer() ?: return
            val id = selectedId ?: return
            when (val result = EncapsulatedSpaceService.rename(serverPlayer, id, rename)) {
                is EncapsulatedSpaceResult.Success -> invalidate()
                is EncapsulatedSpaceResult.Failure -> serverPlayer.displayActionBar(result.translationKey)
            }
        }

        fun deleteSelected() {
            val serverPlayer = serverPlayer() ?: return
            val id = selectedId ?: return
            when (val result = EncapsulatedSpaceService.delete(serverPlayer, id)) {
                is EncapsulatedSpaceResult.Success -> {
                    invalidate()
                    selectedId = spaces().firstOrNull()?.id
                    rename = selectedSpace()?.customName.orEmpty()
                    page = page.coerceAtMost(maxPage())
                }
                is EncapsulatedSpaceResult.Failure -> serverPlayer.displayActionBar(result.translationKey)
            }
        }

        fun teleportSelected() {
            val serverPlayer = serverPlayer() ?: return
            selectedId?.let { id -> TeleporterService.teleportToSpace(serverPlayer, id) }
        }

        fun teleportHub() {
            serverPlayer()?.let(TeleporterService::teleportToHub)
        }

        fun returnOutside() {
            serverPlayer()?.let(TeleporterService::returnOutside)
        }

        fun previousPage() {
            page = (page - 1).coerceAtLeast(0)
        }

        fun nextPage() {
            page = (page + 1).coerceAtMost(maxPage())
        }

        fun pageLabel(): Component = Component.literal("${page + 1}/${maxPage() + 1}")

        fun selectedName(): Component = selectedSpace()?.displayName() ?: Component.translatable("gui.lazy.teleporter.no_selection")

        fun selectedUuidLabel(): Component =
            selectedSpace()?.let { space -> Component.translatable("gui.lazy.teleporter.uuid", space.shortId) }
                ?: Component.empty()

        fun selectedUuidTooltip(): Component = selectedSpace()?.let { space -> Component.literal(space.id.toString()) } ?: Component.empty()

        fun selectedOwner(): Component {
            val serverPlayer = serverPlayer() ?: return Component.empty()
            if (!serverPlayer.hasPermissions(2)) return Component.empty()
            return selectedSpace()?.let { space ->
                Component.translatable("gui.lazy.teleporter.owner", space.ownerId.toString())
            } ?: Component.empty()
        }

        fun deletePrompt(): Component =
            selectedSpace()?.let { space ->
                Component.translatable("gui.lazy.teleporter.delete_prompt", space.displayName(), space.shortId)
            } ?: Component.translatable("gui.lazy.teleporter.no_selection")

        fun renameText(): String = rename

        fun setRenameText(value: String) {
            rename = value
        }

        private fun selectedSpace(): EncapsulatedSpace? = spaces().firstOrNull { space -> space.id == selectedId }

        private fun rowSpace(rowIndex: Int): EncapsulatedSpace? = spaces().getOrNull(page * PAGE_SIZE + rowIndex)

        private fun maxPage(): Int = ((spaces().size - 1).coerceAtLeast(0)) / PAGE_SIZE

        private fun spaces(): List<EncapsulatedSpace> {
            val serverPlayer = serverPlayer() ?: return emptyList()
            if (cacheTick != serverPlayer.tickCount) {
                cachedSpaces = EncapsulatedSpaceService.listFor(serverPlayer)
                cacheTick = serverPlayer.tickCount
            }
            return cachedSpaces
        }

        private fun invalidate() {
            cacheTick = Int.MIN_VALUE
        }

        private fun serverPlayer(): ServerPlayer? = player as? ServerPlayer
    }

    private fun <T : UIElement> raw(element: T): UIBuilder<T> =
        object : UIBuilder<T> {
            override fun build(): T = element
        }

    private const val PAGE_SIZE = 64
    private const val LEFT_MOUSE_BUTTON = 0
}
