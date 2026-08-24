package com.typewritermc.roadnetwork.content

import com.github.retrooper.packetevents.protocol.player.InteractionHand
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RoadNetworkNodesComponentTest {
    @Test
    fun `accepts main-hand attack for node removal`() {
        assertTrue(
            isRoadNetworkNodeInteraction(
                InteractionHand.MAIN_HAND,
                WrapperPlayClientInteractEntity.InteractAction.ATTACK,
            ),
        )
    }

    @Test
    fun `accepts main-hand interact-at for node selection`() {
        assertTrue(
            isRoadNetworkNodeInteraction(
                InteractionHand.MAIN_HAND,
                WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT,
            ),
        )
    }

    @Test
    fun `ignores duplicate generic interaction packet`() {
        assertFalse(
            isRoadNetworkNodeInteraction(
                InteractionHand.MAIN_HAND,
                WrapperPlayClientInteractEntity.InteractAction.INTERACT,
            ),
        )
    }

    @Test
    fun `ignores off-hand interaction`() {
        assertFalse(
            isRoadNetworkNodeInteraction(
                InteractionHand.OFF_HAND,
                WrapperPlayClientInteractEntity.InteractAction.INTERACT_AT,
            ),
        )
    }
}
