package rhx.lazy.feature.teleporter

import com.lowdragmc.lowdraglib2.gui.factory.PlayerUIMenuType
import com.lowdragmc.lowdraglib2.gui.sync.bindings.impl.DataBindingBuilder
import com.lowdragmc.lowdraglib2.gui.texture.ItemStackTexture
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI
import com.lowdragmc.lowdraglib2.gui.ui.UI
import com.lowdragmc.lowdraglib2.gui.ui.UIBuilder
import com.lowdragmc.lowdraglib2.gui.ui.UIContainer
import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import com.lowdragmc.lowdraglib2.gui.ui.column
import com.lowdragmc.lowdraglib2.gui.ui.element
import com.lowdragmc.lowdraglib2.gui.ui.elements.BindableValue
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button
import com.lowdragmc.lowdraglib2.gui.ui.elements.Dialog
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField
import com.lowdragmc.lowdraglib2.gui.ui.elements.button
import com.lowdragmc.lowdraglib2.gui.ui.elements.label
import com.lowdragmc.lowdraglib2.gui.ui.elements.textField
import com.lowdragmc.lowdraglib2.gui.ui.row
import com.lowdragmc.lowdraglib2.gui.ui.style.StylesheetManager
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import rhx.lazy.core.displayActionBar
import rhx.lazy.core.lazyId
import rhx.lazy.core.ui.closeLazyModal
import rhx.lazy.core.ui.lazyModalDialog
import rhx.lazy.core.ui.openLazyModal
import rhx.lazy.feature.voidworld.EncapsulatedSpace
import rhx.lazy.feature.voidworld.EncapsulatedSpaceResult
import rhx.lazy.feature.voidworld.EncapsulatedSpaceService
import rhx.lazy.feature.voidworld.VoidWorldKeys
import rhx.lazy.integration.api.LazyInternalApi
import java.util.Locale
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
        val bindings = ArrayList<UIElement>()
        lateinit var createDialog: Dialog
        lateinit var renameDialog: Dialog
        lateinit var deleteDialog: Dialog
        lateinit var renameField: TextField

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
                row({ cls = { +"lazy-teleporter__toolbar" } }) {
                    button(
                        {
                            text = Component.literal("+")
                            cls = {
                                +"lazy-teleporter__tool-button"
                                +"lazy-teleporter__create"
                            }
                            style = { tooltips(Component.translatable("gui.lazy.teleporter.create")) }
                            onClick = { createDialog.openLazyModal() }
                        },
                    )
                    val previous = actionButton(Component.literal("‹"), "gui.lazy.teleporter.previous_page", model::previousPage)
                    addChild(raw(boundLabel(model::pageLabel, "lazy-teleporter__page")))
                    val next = actionButton(Component.literal("›"), "gui.lazy.teleporter.next_page", model::nextPage)
                    bindings += activeBinding(model::hasPreviousPage, previous)
                    bindings += activeBinding(model::hasNextPage, next)
                    element({ cls = { +"lazy-teleporter__toolbar-spacer" } })
                    iconActionButton(Items.COMPASS, "gui.lazy.teleporter.hub", serverAction = model::teleportHub)
                    val returnButton =
                        iconActionButton(
                            Items.GRASS_BLOCK,
                            "gui.lazy.teleporter.return",
                            actionClass = "lazy-teleporter__return",
                            serverAction = model::returnOutside,
                        )
                    bindings += activeBinding(model::isInVoid, returnButton)
                }
                row({ cls = { +"lazy-teleporter__body" } }) {
                    column({ cls = { +"lazy-teleporter__left" } }) {
                        textField(
                            {
                                text = ""
                                placeholder("gui.lazy.teleporter.search")
                                cls = { +"lazy-teleporter__search" }
                            },
                        ).element.apply {
                            setAnyString()
                            bind(
                                DataBindingBuilder
                                    .string(model::filterText, model::setFilterText)
                                    .initialValue("")
                                    .build(),
                            )
                        }
                        column({ cls = { +"lazy-teleporter__list" } }) {
                            repeat(PAGE_SIZE) { rowIndex ->
                                val rowButton = Button()
                                rowButton.noText()
                                rowButton.addClass("lazy-teleporter__space-row")
                                rowButton.addChild(boundLabel({ model.rowLabel(rowIndex) }, "lazy-teleporter__space-label"))
                                rowButton.setOnServerClick { event ->
                                    if (event.button == LEFT_MOUSE_BUTTON) model.selectRow(rowIndex)
                                }
                                bindings += displayBinding({ model.rowVisible(rowIndex) }, rowButton)
                                bindings += selectedBinding({ model.rowSelected(rowIndex) }, rowButton)
                                addChild(raw(rowButton))
                            }
                        }
                    }
                    val details =
                        column({ cls = { +"lazy-teleporter__details" } }) {
                            addChild(raw(boundLabel(model::selectedName, "lazy-teleporter__selected-name")))
                            addChild(raw(boundLabel(model::selectedOwner, "lazy-teleporter__owner")))
                            addChild(raw(boundLabel(model::selectedUuidLabel, "lazy-teleporter__uuid")))
                            row({ cls = { +"lazy-teleporter__selection-actions" } }) {
                                iconActionButton(Items.NAME_TAG, "gui.lazy.teleporter.rename", clientAction = {
                                    renameDialog.openLazyModal(renameField)
                                })
                                iconActionButton(
                                    Items.ENDER_PEARL,
                                    "gui.lazy.teleporter.travel",
                                    serverAction = model::teleportSelected,
                                )
                                iconActionButton(
                                    Items.BARRIER,
                                    "gui.lazy.teleporter.delete",
                                    actionClass = "lazy-teleporter__delete",
                                    clientAction = {
                                        deleteDialog.openLazyModal()
                                    },
                                )
                            }
                        }.element
                    bindings += displayBinding(model::hasSelection, details)
                }
            }

        createDialog = createCreateDialog(model)
        val renameModal = createRenameDialog(model)
        renameDialog = renameModal.dialog
        renameField = renameModal.field
        deleteDialog = createDeleteDialog(model)
        bindings.forEach(root::addChild)
        root.addChild(createDialog)
        root.addChild(renameDialog)
        root.addChild(deleteDialog)
        return ModularUI(UI.of(root, StylesheetManager.MC, stylesheet), player)
    }

    private fun createCreateDialog(model: Model): Dialog {
        lateinit var dialog: Dialog
        val content =
            modalContent(
                titleKey = "gui.lazy.teleporter.create_title",
                prompt = { Component.translatable("gui.lazy.teleporter.create_prompt") },
            ) {
                modalButton("gui.cancel", clientAction = { dialog.closeLazyModal() })
                modalButton(
                    "gui.lazy.teleporter.confirm_create",
                    clientAction = { dialog.closeLazyModal() },
                    serverAction = model::createSpace,
                )
            }
        dialog = lazyModalDialog(content)
        return dialog
    }

    private fun createRenameDialog(model: Model): RenameModal {
        lateinit var dialog: Dialog
        lateinit var field: TextField
        val content =
            element({ cls = { +"lazy-teleporter__modal" } }) {
                label(
                    {
                        text = Component.translatable("gui.lazy.teleporter.rename_title")
                        cls = { +"lazy-teleporter__modal-title" }
                    },
                )
                label(
                    {
                        text = Component.translatable("gui.lazy.teleporter.rename_prompt")
                        cls = { +"lazy-teleporter__modal-text" }
                    },
                )
                field =
                    textField(
                        {
                            text = ""
                            cls = { +"lazy-teleporter__rename-field" }
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
                row({ cls = { +"lazy-teleporter__modal-actions" } }) {
                    modalButton("gui.cancel", clientAction = { dialog.closeLazyModal() })
                    modalButton(
                        "gui.lazy.teleporter.confirm_rename",
                        clientAction = { dialog.closeLazyModal() },
                        serverAction = model::renameSelected,
                    )
                }
            }
        dialog = lazyModalDialog(content)
        return RenameModal(dialog, field)
    }

    private fun createDeleteDialog(model: Model): Dialog {
        lateinit var dialog: Dialog
        val content =
            modalContent(
                titleKey = "gui.lazy.teleporter.delete_title",
                prompt = model::deletePrompt,
            ) {
                modalButton("gui.cancel", clientAction = { dialog.closeLazyModal() })
                modalButton(
                    "gui.lazy.teleporter.confirm_delete",
                    actionClass = "lazy-teleporter__delete",
                    clientAction = { dialog.closeLazyModal() },
                    serverAction = model::deleteSelected,
                )
            }
        dialog = lazyModalDialog(content)
        return dialog
    }

    private fun modalContent(
        titleKey: String,
        prompt: () -> Component,
        actions: UIContainer<*, *>.() -> Unit,
    ): UIElement =
        element({ cls = { +"lazy-teleporter__modal" } }) {
            label(
                {
                    text = Component.translatable(titleKey)
                    cls = { +"lazy-teleporter__modal-title" }
                },
            )
            addChild(raw(boundLabel(prompt, "lazy-teleporter__modal-text")))
            row({ cls = { +"lazy-teleporter__modal-actions" } }, actions)
        }

    private fun UIContainer<*, *>.modalButton(
        translationKey: String,
        actionClass: String? = null,
        clientAction: () -> Unit,
        serverAction: (() -> Unit)? = null,
    ) {
        button(
            {
                text = Component.translatable(translationKey)
                cls = {
                    +"lazy-teleporter__modal-button"
                    actionClass?.let { +it }
                }
                onClick = { clientAction() }
                serverAction?.let { action ->
                    onServerClick = { event -> if (event.button == LEFT_MOUSE_BUTTON) action() }
                }
            },
        )
    }

    private fun UIContainer<*, *>.actionButton(
        text: Component,
        tooltipKey: String,
        action: () -> Unit,
    ): Button =
        button(
            {
                this.text = text
                cls = { +"lazy-teleporter__tool-button" }
                style = { tooltips(Component.translatable(tooltipKey)) }
                onServerClick = { event -> if (event.button == LEFT_MOUSE_BUTTON) action() }
            },
        ).element

    private fun UIContainer<*, *>.iconActionButton(
        icon: Item,
        tooltipKey: String,
        actionClass: String? = null,
        clientAction: (() -> Unit)? = null,
        serverAction: (() -> Unit)? = null,
    ): Button =
        button(
            {
                noText()
                cls = {
                    +"lazy-teleporter__icon-button"
                    actionClass?.let { +it }
                }
                style = { tooltips(Component.translatable(tooltipKey)) }
                clientAction?.let { action -> onClick = { action() } }
                serverAction?.let { action ->
                    onServerClick = { event -> if (event.button == LEFT_MOUSE_BUTTON) action() }
                }
            },
        ).element.apply { addPreIcon(ItemStackTexture(ItemStack(icon))) }

    private fun boundLabel(
        supplier: () -> Component,
        cssClass: String,
    ): Label =
        Label().apply {
            setValue(Component.empty())
            addClass(cssClass)
            bind(DataBindingBuilder.componentS2C(supplier).initialValue(Component.empty()).build())
        }

    private fun displayBinding(
        supplier: () -> Boolean,
        target: UIElement,
    ): BindableValue<Boolean> = booleanBinding(supplier) { display -> target.setDisplay(display) }

    private fun activeBinding(
        supplier: () -> Boolean,
        target: UIElement,
    ): BindableValue<Boolean> = booleanBinding(supplier) { active -> target.setActive(active) }

    private fun selectedBinding(
        supplier: () -> Boolean,
        target: UIElement,
    ): BindableValue<Boolean> =
        booleanBinding(supplier) { selected ->
            if (selected) target.addClass(SELECTED_ROW_CLASS) else target.removeClass(SELECTED_ROW_CLASS)
        }

    private fun booleanBinding(
        supplier: () -> Boolean,
        listener: (Boolean) -> Unit,
    ): BindableValue<Boolean> =
        BindableValue(false).apply {
            setDisplay(false)
            registerValueListener(listener)
            bind(DataBindingBuilder.boolS2C(supplier).initialValue(false).build())
        }

    private class Model(
        private val player: Player,
    ) {
        private var selectedId: UUID? = null
        private var page = 0
        private var filter = ""
        private var rename = ""
        private var cacheTick = Int.MIN_VALUE
        private var cachedSpaces: List<EncapsulatedSpace> = emptyList()

        init {
            serverPlayer()?.let { serverPlayer ->
                val spaces = spaces()
                val preferred = serverPlayer.getData(TeleporterRegistries.playerState.get()).selectedSpaceId
                selectedId = spaces.firstOrNull { space -> space.id == preferred }?.id ?: spaces.firstOrNull()?.id
                rename = selectedSpace()?.editableName().orEmpty()
            }
        }

        fun rowLabel(rowIndex: Int): Component = rowSpace(rowIndex)?.displayName() ?: Component.empty()

        fun rowVisible(rowIndex: Int): Boolean = rowSpace(rowIndex) != null

        fun rowSelected(rowIndex: Int): Boolean = rowSpace(rowIndex)?.id == selectedId

        fun selectRow(rowIndex: Int) {
            val space = rowSpace(rowIndex) ?: return
            selectedId = space.id
            rename = space.editableName()
        }

        fun createSpace() {
            val serverPlayer = serverPlayer() ?: return
            when (val result = EncapsulatedSpaceService.create(serverPlayer)) {
                is EncapsulatedSpaceResult.Success -> {
                    invalidate()
                    result.space?.let { space ->
                        selectedId = space.id
                        rename = ""
                        filter = ""
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
                    rename = selectedSpace()?.editableName().orEmpty()
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
            serverPlayer()?.takeIf { isInVoid() }?.let(TeleporterService::returnOutside)
        }

        fun previousPage() {
            page = (page - 1).coerceAtLeast(0)
        }

        fun nextPage() {
            page = (page + 1).coerceAtMost(maxPage())
        }

        fun hasPreviousPage(): Boolean = page > 0

        fun hasNextPage(): Boolean = page < maxPage()

        fun pageLabel(): Component = Component.literal("${page + 1}/${maxPage() + 1}")

        fun hasSelection(): Boolean = selectedSpace() != null

        fun isInVoid(): Boolean = serverPlayer()?.level()?.dimension() == VoidWorldKeys.voidLevel

        fun selectedName(): Component = selectedSpace()?.displayName() ?: Component.empty()

        fun selectedUuidLabel(): Component =
            selectedSpace()?.let { space -> Component.translatable("gui.lazy.teleporter.uuid", space.shortId) }
                ?: Component.empty()

        fun selectedOwner(): Component {
            val serverPlayer = serverPlayer() ?: return Component.empty()
            if (!serverPlayer.hasPermissions(OP_PERMISSION_LEVEL)) return Component.empty()
            return selectedSpace()?.let { space ->
                val ownerName =
                    serverPlayer.server.playerList
                        .getPlayer(space.ownerId)
                        ?.gameProfile
                        ?.name
                        ?: serverPlayer.server.profileCache
                            ?.get(space.ownerId)
                            ?.map { profile -> profile.name }
                            ?.orElse(space.shortId)
                        ?: space.shortId
                Component.translatable("gui.lazy.teleporter.owner", ownerName)
            } ?: Component.empty()
        }

        fun deletePrompt(): Component =
            selectedSpace()?.let { space ->
                Component.translatable("gui.lazy.teleporter.delete_prompt", space.displayName(), space.shortId)
            } ?: Component.translatable("gui.lazy.teleporter.no_selection")

        fun filterText(): String = filter

        fun setFilterText(value: String) {
            filter = value
            page = 0
        }

        fun renameText(): String = rename

        fun setRenameText(value: String) {
            rename = value
        }

        private fun selectedSpace(): EncapsulatedSpace? = spaces().firstOrNull { space -> space.id == selectedId }

        private fun rowSpace(rowIndex: Int): EncapsulatedSpace? = filteredSpaces().getOrNull(page * PAGE_SIZE + rowIndex)

        private fun maxPage(): Int = ((filteredSpaces().size - 1).coerceAtLeast(0)) / PAGE_SIZE

        private fun filteredSpaces(): List<EncapsulatedSpace> {
            val query = filter.trim().lowercase(Locale.ROOT)
            if (query.isEmpty()) return spaces()
            return spaces().filter { space ->
                space
                    .displayName()
                    .string
                    .lowercase(Locale.ROOT)
                    .contains(query)
            }
        }

        private fun EncapsulatedSpace.editableName(): String = customName ?: displayName().string

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

    private data class RenameModal(
        val dialog: Dialog,
        val field: TextField,
    )

    private fun <T : UIElement> raw(element: T): UIBuilder<T> =
        object : UIBuilder<T> {
            override fun build(): T = element
        }

    private const val PAGE_SIZE = 5
    private const val LEFT_MOUSE_BUTTON = 0
    private const val OP_PERMISSION_LEVEL = 2
    private const val SELECTED_ROW_CLASS = "lazy-teleporter__space-row--selected"
}
