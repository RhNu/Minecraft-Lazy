package rhx.lazy.feature.replicator

import com.lowdragmc.lowdraglib2.gui.ui.UIElement
import rhx.lazy.core.resource.ResourceKind
import rhx.lazy.core.resource.ResourceVariant
import rhx.lazy.integration.api.LazyInternalApi

/** Client-side sink that sends a directly dragged resource identity to the server menu. */
@LazyInternalApi
public interface ReplicatorGhostIngredientSink {
    fun <V : ResourceVariant> select(
        kind: ResourceKind<V>,
        variant: V,
    )
}

/** Optional integrations attach their ingredient-viewer ghost target to the shared resource slot. */
@LazyInternalApi
public fun interface ReplicatorGhostIngredientExtension {
    public fun install(
        element: UIElement,
        sink: ReplicatorGhostIngredientSink,
    )
}

@LazyInternalApi
public object ReplicatorGhostIngredientExtensions {
    private val extensions = mutableListOf<ReplicatorGhostIngredientExtension>()

    public fun register(extension: ReplicatorGhostIngredientExtension) {
        extensions += extension
    }

    internal fun install(
        element: UIElement,
        sink: ReplicatorGhostIngredientSink,
    ) {
        extensions.forEach { it.install(element, sink) }
    }
}
