package com.typewritermc.entity.entries.activity

private const val LEVEL_HEAD_DISTANCE_SQUARED = 0.75 * 0.75

internal fun walkingLookRotation(
    current: LookDirection,
    dx: Double,
    dy: Double,
    dz: Double,
    yawVelocity: Velocity,
    pitchVelocity: Velocity,
    smoothTime: Float = 0.2f,
): Pair<Float, Float> {
    if (dx * dx + dz * dz < LEVEL_HEAD_DISTANCE_SQUARED) {
        return current.yaw to smoothDamp(current.pitch, 0f, pitchVelocity, smoothTime)
    }

    return updateLookDirection(
        current,
        LookDirection(getLookYaw(dx, dz), getLookPitch(dx, dy, dz)),
        yawVelocity,
        pitchVelocity,
        smoothTime,
    )
}
