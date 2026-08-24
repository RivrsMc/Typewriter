package com.typewritermc.entity.entries.audience

import com.typewritermc.core.entries.Ref
import com.typewritermc.core.entries.emptyRef
import com.typewritermc.core.utils.point.Position
import com.typewritermc.core.utils.point.lerp
import com.typewritermc.engine.paper.entry.entity.IndividualActivityContext
import com.typewritermc.engine.paper.entry.entity.toProperty
import com.typewritermc.entity.entries.activity.getLookPitch
import com.typewritermc.entity.entries.activity.getLookYaw
import com.typewritermc.entity.entries.activity.NavigationActivityTaskState
import com.typewritermc.roadnetwork.RoadNetworkEntry
import com.typewritermc.roadnetwork.entries.*
import com.typewritermc.roadnetwork.gps.GPSEdge
import org.bukkit.entity.Player
import java.time.Duration
import kotlin.math.sqrt

class PathFindingPathStreamProducer(
    player: Player,
    id: String,
    roadNetwork: Ref<RoadNetworkEntry>,
    startPosition: (Player) -> Position,
    endPosition: (Player) -> Position,
    refreshDuration: Duration,
    val speed: Double = 0.5,
    displaySupplier: PathStreamDisplaysSupplier,
    maxActiveStreams: Int = Int.MAX_VALUE,
) : PathStreamProducer(
    player,
    id,
    roadNetwork,
    startPosition,
    endPosition,
    refreshDuration,
    displaySupplier,
    maxActiveStreams,
) {
    constructor(
        player: Player,
        ref: Ref<PathStreamDisplayEntry>,
        roadNetwork: Ref<RoadNetworkEntry>,
        startPosition: (Player) -> Position,
        endPosition: (Player) -> Position,
        refreshDuration: Duration = Duration.ofMillis(1200),
        speed: Double = 0.5,
        displayEntries: List<Ref<PathStreamDisplayEntry>>,
        maxActiveStreams: Int = Int.MAX_VALUE,
    ) : this(
        player,
        ref.id,
        roadNetwork,
        startPosition,
        endPosition,
        refreshDuration,
        speed,
        { displayEntries.createDisplays(it) },
        maxActiveStreams,
    )

    override suspend fun refreshPath(): PathStream? {
        val (start, end) = points() ?: return null
        val edges = findEdges() ?: return null
        val visibleEdges = edges.filterVisible(start, end)
        if (visibleEdges.isEmpty()) return null
        return PathFindingPathStream(
            displaySupplier(player),
            player,
            roadNetwork,
            visibleEdges,
            speed
        )
    }
}

class PathFindingPathStream(
    displays: List<PathStreamDisplay>,
    val player: Player,
    val roadNetwork: Ref<RoadNetworkEntry>,
    private val edges: List<GPSEdge>,
    private val speed: Double,
) : PathStream(displays) {
    private var navigator: PathStreamEdgeNavigator
    private var currentEdgeIndex = 0
        set(value) {
            field = value
            startTime = System.currentTimeMillis()
        }
    private val currentEdge: GPSEdge
        get() = edges[currentEdgeIndex.coerceIn(0 until edges.size)]

    init {
        require(edges.isNotEmpty()) { "There must be at least 1 edge for the entity to walk" }
        navigator = createNavigator(currentEdge, currentEdge.start)
    }

    override fun forwardPath(): Position {
        require(currentEdgeIndex < edges.size) { "No more edges to walk on" }
        navigator.tick()
        val position = navigator.position()

        if (!navigator.isComplete()) return position
        navigator.dispose()
        currentEdgeIndex++
        if (!shouldContinue()) return position
        navigator = createNavigator(currentEdge, position)
        return position
    }

    private fun createNavigator(edge: GPSEdge, startPosition: Position): PathStreamEdgeNavigator {
        return if (edge.isFastTravel) {
            LinearFastTravelNavigator(edge, speed)
        } else {
            WalkingPathStreamNavigator(player, roadNetwork, edge, startPosition, speed)
        }
    }

    override fun shouldContinue(): Boolean {
        return currentEdgeIndex < edges.size
    }

    override fun dispose() {
        super.dispose()
        navigator.dispose()
    }
}

internal interface PathStreamEdgeNavigator {
    fun tick()
    fun position(): Position
    fun isComplete(): Boolean
    fun dispose() {}
}

private class WalkingPathStreamNavigator(
    private val player: Player,
    roadNetwork: Ref<RoadNetworkEntry>,
    edge: GPSEdge,
    startPosition: Position,
    speed: Double,
) : PathStreamEdgeNavigator {
    private val delegate = NavigationActivityTaskState.Walking(
        roadNetwork,
        edge,
        startPosition.toProperty(),
        speed.toFloat(),
        rotationLookAhead = 0,
    )

    override fun tick() {
        delegate.tick(IndividualActivityContext(emptyRef(), player, isViewed = true))
    }

    override fun position(): Position = delegate.position().toPosition()

    override fun isComplete(): Boolean = delegate.isComplete()

    override fun dispose() = delegate.dispose()
}

/**
 * Renders a manual fast-travel edge as a straight, oriented visual bridge.
 * The speed keeps the existing animated display semantics (blocks advanced per server tick).
 */
internal class LinearFastTravelNavigator(
    private val edge: GPSEdge,
    speed: Double,
) : PathStreamEdgeNavigator {
    private val dx = edge.end.x - edge.start.x
    private val dy = edge.end.y - edge.start.y
    private val dz = edge.end.z - edge.start.z
    private val distance = sqrt(dx * dx + dy * dy + dz * dz)
    private val step = speed.coerceAtLeast(MINIMUM_STEP)
    private val yaw = getLookYaw(dx, dz)
    private val pitch = getLookPitch(dx, dy, dz)
    private var travelled = 0.0

    override fun tick() {
        travelled = (travelled + step).coerceAtMost(distance)
    }

    override fun position(): Position {
        val progress = if (distance == 0.0) 1.0 else travelled / distance
        return edge.start.lerp(edge.end, progress).withRotation(yaw, pitch)
    }

    override fun isComplete(): Boolean = travelled >= distance

    private companion object {
        const val MINIMUM_STEP = 0.001
    }
}
