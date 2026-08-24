package com.typewritermc.roadnetwork.gps

import com.extollit.gaming.ai.path.HydrazinePathFinder
import com.extollit.gaming.ai.path.model.*
import com.typewritermc.core.entries.Ref
import com.typewritermc.core.utils.point.Position
import com.typewritermc.core.utils.point.Vector
import com.typewritermc.core.utils.point.distanceSqrt
import com.typewritermc.core.utils.point.lerp
import com.typewritermc.engine.paper.entry.entity.toProperty
import com.typewritermc.roadnetwork.RoadNetworkEntry
import com.typewritermc.roadnetwork.RoadNode
import com.typewritermc.roadnetwork.pathfinding.PFEmptyEntity
import com.typewritermc.roadnetwork.pathfinding.PFInstanceSpace
import com.typewritermc.roadnetwork.pathfinding.instanceSpace
import com.typewritermc.roadnetwork.pathfinding.pathfindingYOffset
import com.typewritermc.roadnetwork.roadNetworkMaxDistance
import kotlin.math.ceil
import kotlin.math.sqrt

interface GPS {
    val roadNetwork: Ref<RoadNetworkEntry>
    suspend fun findPath(): Result<List<GPSEdge>>
}

data class GPSEdge(
    val start: Position,
    val end: Position,
    val weight: Double,
    /**
     * The number of blocks the path is long.
     */
    val length: Double,
) {
    val isFastTravel: Boolean
        get() = weight == 0.0
}

/**
 * Builds a deterministic visual bridge for a fast-travel edge.
 *
 * Fast-travel connections deliberately bypass physical pathfinding. Path stream displays still need
 * positions to render between both nodes, otherwise they try Hydrazine again and fail on the exact
 * obstacle the manual connection was meant to cross (water, gaps, portals, ...).
 *
 * Cross-world edges cannot be represented as a continuous visual line and are therefore omitted.
 */
fun GPSEdge.fastTravelVisualPath(spacing: Double = 1.0): List<Position> {
    require(spacing > 0.0) { "Fast-travel visual spacing must be positive" }
    if (!isFastTravel || start.world != end.world) return emptyList()

    val dx = end.x - start.x
    val dy = end.y - start.y
    val dz = end.z - start.z
    val distance = sqrt(dx * dx + dy * dy + dz * dz)
    if (distance == 0.0) return listOf(start)

    val segments = ceil(distance / spacing).toInt().coerceAtLeast(1)
    return (0..segments).map { index ->
        start.lerp(end, index.toDouble() / segments)
    }
}

fun roadNetworkFindPath(
    start: RoadNode,
    end: RoadNode,
    entity: IPathingEntity = PFEmptyEntity(
        start.position.add(0.0, pathfindingYOffset.toDouble(), 0.0).toProperty(),
        searchRange = roadNetworkMaxDistance.toFloat()
    ),
    instance: PFInstanceSpace = start.position.world.instanceSpace,
    nodes: List<RoadNode> = emptyList(),
    negativeNodes: List<RoadNode> = emptyList(),
): IPath? {
    return roadNetworkFindPath(start, end, HydrazinePathFinder(entity, instance), nodes, negativeNodes)
}

fun roadNetworkFindPath(
    start: RoadNode,
    end: RoadNode,
    pathfinder: HydrazinePathFinder,
    nodes: List<RoadNode> = emptyList(),
    negativeNodes: List<RoadNode> = emptyList(),
): IPath? {
    val interestingNodes = nodes.filter {
        if (it.id == start.id) return@filter false
        if (it.id == end.id) return@filter false
        true
    }
    val interestingNegativeNodes = negativeNodes.filter {
        val distance = start.position.distanceSqrt(it.position) ?: 0.0
        distance > it.radius * it.radius && distance < roadNetworkMaxDistance * roadNetworkMaxDistance
    }

    val additionalRadius = pathfinder.subject().width().toDouble()

    // We want to avoid going through negative nodes
    if (interestingNegativeNodes.isNotEmpty()) {
        pathfinder.withGraphNodeFilter { node ->
            if (node.isInRangeOf(interestingNegativeNodes, additionalRadius)) {
                return@withGraphNodeFilter Passibility.dangerous
            }
            node.passibility()
        }
    }

    // When the pathfinder wants to go through another intermediary node, we know that we probably want to use that.
    // So we don't want this edge to be used.
    val path = pathfinder.computePathTo(
        end.position.x,
        end.position.y + pathfindingYOffset,
        end.position.z
    ) ?: return null
    if (interestingNodes.isNotEmpty() && path.any { it.isInRangeOf(interestingNodes, additionalRadius) }) {
        return null
    }

    return path
}

fun INode.isInRangeOf(roadNodes: List<RoadNode>, additionalRadius: Double = 0.0): Boolean {
    return roadNodes.any { roadNode ->
        val point = this.coordinates().toVector().mid()
        val radius = roadNode.radius + additionalRadius
        roadNode.position
            .add(0.0, pathfindingYOffset.toDouble(), 0.0)
            .toProperty()
            .distanceSquared(point) <= radius * radius
    }
}

fun Coords.toVector() = Vector(x.toDouble(), y.toDouble(), z.toDouble())
