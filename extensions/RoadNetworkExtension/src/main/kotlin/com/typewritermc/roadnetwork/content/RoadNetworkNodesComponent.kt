package com.typewritermc.roadnetwork.content

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.util.Vector3f
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import com.typewritermc.core.utils.point.Position
import com.typewritermc.core.utils.point.distanceSqrt
import com.typewritermc.engine.paper.content.ComponentContainer
import com.typewritermc.engine.paper.content.ContentComponent
import com.typewritermc.engine.paper.events.AsyncFakeEntityInteract
import com.typewritermc.engine.paper.extensions.packetevents.meta
import com.typewritermc.engine.paper.extensions.packetevents.toPacketItem
import com.typewritermc.engine.paper.plugin
import com.typewritermc.engine.paper.utils.position
import com.typewritermc.engine.paper.utils.toPacketLocation
import lirand.api.extensions.events.unregister
import lirand.api.extensions.server.registerEvents
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import me.tofaa.entitylib.meta.other.InteractionMeta
import me.tofaa.entitylib.wrapper.WrapperEntity
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import kotlin.math.max

private const val ROAD_NODE_SHOW_DISTANCE_SQUARED = 50 * 50

fun <N> ComponentContainer.roadNetworkNodes(
    nodeFetcher: () -> Collection<N>,
    nodePosition: (N) -> Position,
    builder: RoadNetworkNodeDisplayBuilder.(N) -> Unit,
) = +RoadNetworkNodesComponent(nodeFetcher, nodePosition, builder)

class RoadNetworkNodesComponent<N>(
    private val nodeFetcher: () -> Collection<N>,
    private val nodePosition: (N) -> Position,
    private val builder: RoadNetworkNodeDisplayBuilder.(N) -> Unit,
) : ContentComponent, Listener {
    private val nodes = mutableMapOf<N, RoadNetworkNodeDisplay>()
    private var lastRefresh = 0

    private fun refreshNodes(player: Player) {
        val newNodes = nodeFetcher()
            .filter {
                (nodePosition(it).distanceSqrt(player.position)
                    ?: Double.MAX_VALUE) < ROAD_NODE_SHOW_DISTANCE_SQUARED
            }
            .toSet()

        val toRemove = nodes.keys - newNodes
        val toRefresh = nodes.keys.intersect(newNodes)
        val toAdd = newNodes - nodes.keys
        toRemove.forEach { nodes.remove(it)?.dispose() }
        toAdd.forEach { node ->
            nodes[node] = RoadNetworkNodeDisplayBuilder()
                .apply { builder(node) }
                .run {
                    RoadNetworkNodeDisplay().also {
                        it.apply(this, nodePosition(node))
                        it.show(player, nodePosition(node))
                    }
                }
        }
        toRefresh.forEach { node ->
            nodes[node]?.apply(
                RoadNetworkNodeDisplayBuilder().apply { builder(node) },
                nodePosition(node),
            )
        }
        lastRefresh = 0
    }

    override suspend fun initialize(player: Player) {
        plugin.registerEvents(this)
        refreshNodes(player)
    }

    override suspend fun tick(player: Player) {
        if (lastRefresh++ > 20) {
            refreshNodes(player)
        }
    }

    @EventHandler
    private fun onFakeEntityInteract(event: AsyncFakeEntityInteract) {
        if (!isRoadNetworkNodeInteraction(event.hand, event.action)) return
        nodes.values.firstOrNull { it.entityId == event.entityId }?.interact()
    }

    override suspend fun dispose(player: Player) {
        unregister()
        nodes.values.forEach { it.dispose() }
        nodes.clear()
    }
}

internal fun isRoadNetworkNodeInteraction(
    hand: InteractionHand,
    action: WrapperPlayClientInteractEntity.InteractAction,
): Boolean {
    return hand == InteractionHand.MAIN_HAND &&
        action != WrapperPlayClientInteractEntity.InteractAction.INTERACT
}

class RoadNetworkNodeDisplayBuilder {
    var item: ItemStack = ItemStack(Material.STONE)
    var glow: TextColor? = null
    var interaction: () -> Unit = {}
    var scale: Vector3f = Vector3f(1.0f, 1.0f, 1.0f)

    fun onInteract(action: () -> Unit) {
        interaction = action
    }
}

private class RoadNetworkNodeDisplay {
    private val itemDisplay = WrapperEntity(EntityTypes.ITEM_DISPLAY)
    private val interaction = WrapperEntity(EntityTypes.INTERACTION)
    private var onInteract: () -> Unit = {}

    val entityId: Int
        get() = interaction.entityId

    fun apply(builder: RoadNetworkNodeDisplayBuilder, position: Position) {
        itemDisplay.meta<ItemDisplayMeta> {
            item = builder.item.toPacketItem()
            isGlowing = builder.glow != null
            glowColorOverride = builder.glow?.value() ?: -1
            scale = builder.scale
            positionRotationInterpolationDuration = 30
        }
        interaction.meta<InteractionMeta> {
            width = max(builder.scale.x, builder.scale.z)
            height = builder.scale.y
        }
        onInteract = builder.interaction
        if (itemDisplay.isSpawned) {
            itemDisplay.teleport(position.toPacketLocation())
        }
        if (
            interaction.isSpawned &&
            (interaction.location.x != position.x ||
                interaction.location.y != position.y - builder.scale.y / 2 ||
                interaction.location.z != position.z)
        ) {
            interaction.teleport(
                position
                    .withY { it - builder.scale.y / 2 }
                    .toPacketLocation(),
            )
        }
    }

    fun show(player: Player, position: Position) {
        itemDisplay.addViewer(player.uniqueId)
        itemDisplay.spawn(position.toPacketLocation())
        interaction.addViewer(player.uniqueId)
        interaction.spawn(position.toPacketLocation())
    }

    fun interact() {
        onInteract()
    }

    fun dispose() {
        itemDisplay.despawn()
        itemDisplay.remove()
        interaction.despawn()
        interaction.remove()
    }
}
