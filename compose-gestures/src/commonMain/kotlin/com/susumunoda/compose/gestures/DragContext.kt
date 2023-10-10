package com.susumunoda.compose.gestures

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

data class DragOptions(
    val onDragScaleX: Float = 1.0f,
    val onDragScaleY: Float = 1.0f,
    val onDropScaleX: Float = 1.0f,
    val onDropScaleY: Float = 1.0f,
    val snapPosition: SnapPosition? = null
)

data class DropOptions(
    val maxDragTargets: Int = 1
)

enum class SnapPosition(val calculateOffset: (Rect) -> Offset) {
    TOP_LEFT(Rect::topLeft),
    TOP_CENTER(Rect::topCenter),
    TOP_RIGHT(Rect::topRight),
    CENTER_LEFT(Rect::centerLeft),
    CENTER(Rect::center),
    CENTER_RIGHT(Rect::centerRight),
    BOTTOM_LEFT(Rect::bottomLeft),
    BOTTOM_CENTER(Rect::bottomCenter),
    BOTTOM_RIGHT(Rect::bottomRight)
}

enum class DragTargetStatus { NONE, DRAGGED, DROPPED }
enum class DropTargetStatus { NONE, HOVERED, DROPPED }

class DragContext<T> {
    private inner class DragTargetState(
        val data: T,
        val status: MutableState<DragTargetStatus> = mutableStateOf(DragTargetStatus.NONE),
        val offset: MutableState<Offset> = mutableStateOf(Offset.Zero),
        val dropTargets: MutableSet<DropTargetState> = mutableSetOf()
    ) {
        fun resetState() {
            status.value = DragTargetStatus.NONE
            offset.value = Offset.Zero
        }

        fun detachFromDropTargets() {
            // Update any attached drop targets to no longer be associated with this drag target
            dropTargets.forEach { dropTarget ->
                dropTarget.dragTargets.remove(this)
                // Update state - e.g. update hover state in case this was the only drag target
                dropTarget.updateState()
                // Notify drop target of the change
                dropTarget.onDragTargetRemoved(data)
            }
            dropTargets.clear()
        }

        fun onDispose() = detachFromDropTargets()
    }

    private val dragTargetStates = mutableSetOf<DragTargetState>()

    fun resetDragTargets() {
        dragTargetStates.forEach {
            // Detach from and update any associated drop targets
            it.detachFromDropTargets()
            // Reset state - e.g. return to the initial position
            it.resetState()
        }
    }

    @Composable
    private fun rememberDragTargetState(
        data: T,
        content: @Composable (DragTargetStatus) -> Unit
    ): DragTargetState {
        val dragTargetState = remember(data, content) { DragTargetState(data) }
        DisposableEffect(data, content) {
            dragTargetStates.add(dragTargetState)
            onDispose {
                dragTargetState.onDispose()
                dragTargetStates.remove(dragTargetState)
            }
        }
        return dragTargetState
    }

    @Composable
    fun DragTarget(
        data: T,
        options: DragOptions = DragOptions(),
        content: @Composable (DragTargetStatus) -> Unit
    ) {
        val dragTargetState = rememberDragTargetState(data, content)
        val statusState = dragTargetState.status
        val offsetState = dragTargetState.offset

        Box(
            modifier = Modifier
                .graphicsLayer {
                    when (statusState.value) {
                        DragTargetStatus.DRAGGED -> {
                            scaleX = options.onDragScaleX
                            scaleY = options.onDragScaleY
                        }

                        DragTargetStatus.DROPPED -> {
                            scaleX = options.onDropScaleX
                            scaleY = options.onDropScaleY
                        }

                        else -> {
                            scaleX = 1f
                            scaleY = 1f
                        }
                    }
                }
                .offset {
                    val dragOffset = offsetState.value
                    when (statusState.value) {
                        DragTargetStatus.DRAGGED, DragTargetStatus.NONE -> {
                            IntOffset(
                                x = dragOffset.x.roundToInt(),
                                y = dragOffset.y.roundToInt()
                            )
                        }

                        DragTargetStatus.DROPPED -> {
                            // When a drag event happens, scaling may be applied in `graphicsLayer`
                            // above (e.g. scaled down to 50% in size). In such a case, the `onDrag`
                            // callback of `detectDragGestures` will return a drag amount that has
                            // been scaled to accommodate for the applied graphical scaling (in this
                            // example, the returned offset will be 2x what it would normally be
                            // without scaling). When a such a target is dropped and a scale factor
                            // has been defined for the dropped state, then we must first "undo" the
                            // scaling that was applied by `detectDragGestures` for the dragged state,
                            // and then apply the appropriate scaling for the dropped state.
                            IntOffset(
                                x = (dragOffset.x * options.onDragScaleY / options.onDropScaleX).roundToInt(),
                                y = (dragOffset.y * options.onDragScaleY / options.onDropScaleY).roundToInt()
                            )
                        }
                    }
                }
                .onGloballyPositioned { coordinates ->
                    val dragTargetRect = coordinates.boundsInWindow()

                    // Keep drag target and drop targets in sync with each other
                    dropTargetStates.forEach { dropTargetState ->
                        // Link/unlink drag target and drop targets only if the drag target is
                        // actually being moved by the user and not, for example, by an animation
                        if (statusState.value == DragTargetStatus.DRAGGED) {
                            if (dropTargetState.globalRect.contains(dragTargetRect.center)) {
                                // Only associate drag/drop targets if within the configured limits
                                if (dropTargetState.dragTargets.size < dropTargetState.options.maxDragTargets) {
                                    dragTargetState.dropTargets.add(dropTargetState)
                                    dropTargetState.dragTargets.add(dragTargetState)
                                }
                            } else {
                                dragTargetState.dropTargets.remove(dropTargetState)
                                dropTargetState.dragTargets.remove(dragTargetState)
                            }
                        }

                        // Update drop target, e.g. to add or remove a hover indicator
                        dropTargetState.updateState()
                    }

                    // Snap-to-target behavior
                    if (statusState.value == DragTargetStatus.DROPPED && options.snapPosition != null) {
                        // This is a UX decision, but it seems to make the most sense to snap to the
                        // last target that was hovered over (e.g. if drop target A contained drop
                        // target B, and the drag target was dropped into B, then it makes sense to
                        // snap to B instead of A).
                        val lastDropTargetRect = dragTargetState.dropTargets.last().globalRect

                        // If the drag target is in a dropped state with an area of 0, it means that
                        // somehow the drag root (i.e. the composable that it is anchored to) must
                        // have moved at a rapid pace in such a way that the drag target went off
                        // screen without enough time to stay snapped to the drop target. In such a
                        // case, because the drag target now has no global position (i.e. it is not
                        // on the screen), there is no way to calculate the difference in position
                        // between it and the drop target. Therefore, in order to avoid the drag
                        // target being in an unreachable state, temporarily bring it back to its
                        // root position so that it has a positive area again. Once this occurs,
                        // `onGloballyPositioned` will fire again and snap the drag target back to
                        // its drop target.
                        if (dragTargetRect == Rect.Zero) {
                            offsetState.value = Offset.Zero
                        } else {
                            val snapFromOffset =
                                options.snapPosition.calculateOffset(dragTargetRect)
                            val snapToOffset =
                                options.snapPosition.calculateOffset(lastDropTargetRect)
                            val remainingOffset = snapFromOffset - snapToOffset
                            // Don't re-snap if already aligned; otherwise, will result in infinite loop
                            if (remainingOffset.x.roundToInt() != 0 || remainingOffset.y.roundToInt() != 0) {
                                val snapOffset = Offset(
                                    // Currently, `dragOffsetState` always reflects the dragged state's
                                    // scaling (i.e. `onDragScaleX` and `onDragScaleY`). Therefore, after
                                    // calculating the physical pixels that we want to offset by, we have
                                    // to transform the X and Y values based on the drag scaling factors.
                                    // See comment in the call to `Modifier.offset` for more details.
                                    x = remainingOffset.x / options.onDragScaleX,
                                    y = remainingOffset.y / options.onDragScaleY
                                )
                                offsetState.value -= snapOffset
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            statusState.value = DragTargetStatus.DRAGGED
                            // Provide an opportunity for drop targets to respond to drag targets
                            // being dragged back out (e.g. update state to no longer account for
                            // this drag target's data)
                            // Future: Consider what it would mean for incoming data to change after the
                            // initial drop. How to inform drop targets of re-drag events in this case?
                            dragTargetState.dropTargets.forEach { dropTarget ->
                                dropTarget.onDragTargetRemoved(data)
                            }
                        },
                        onDragEnd = {
                            if (dragTargetState.dropTargets.isEmpty()) {
                                statusState.value = DragTargetStatus.NONE
                                // Return to original position
                                offsetState.value = Offset.Zero
                            } else {
                                statusState.value = DragTargetStatus.DROPPED
                                // It is the responsibility of the onDrop callback to update state
                                // in such a way that this DropTarget leaves the composition (if desired).
                                dragTargetState.dropTargets.forEach { dropTarget ->
                                    dropTarget.onDragTargetAdded(data)
                                }
                            }
                        },
                        onDragCancel = {
                            dragTargetState.resetState()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetState.value += dragAmount
                        }
                    )
                }
        ) {
            content(statusState.value)
        }
    }

    private inner class DropTargetState(
        var globalRect: Rect = Rect.Zero,
        val onDragTargetAdded: (T) -> Unit = {},
        val onDragTargetRemoved: (T) -> Unit = {},
        val options: DropOptions = DropOptions(),
        val status: MutableState<DropTargetStatus> = mutableStateOf(DropTargetStatus.NONE),
        val dragTargets: MutableSet<DragTargetState> = mutableSetOf()
    ) {
        fun updateState() {
            var hasDraggedTargets = false
            var hasDroppedTargets = false

            dragTargets.forEach {
                when (it.status.value) {
                    DragTargetStatus.DRAGGED -> hasDraggedTargets = true
                    DragTargetStatus.DROPPED -> hasDroppedTargets = true
                    DragTargetStatus.NONE -> {}
                }
            }

            // Hovered state takes precedence over dropped state. E.g. If a drop target has two drag
            // targets inside of it, one still being dragged and one already dropped, then we want
            // the drop target to be in a hovered state.
            status.value = if (hasDraggedTargets) {
                DropTargetStatus.HOVERED
            } else if (hasDroppedTargets) {
                DropTargetStatus.DROPPED
            } else {
                DropTargetStatus.NONE
            }
        }

        fun detachFromDragTargets() {
            // Update any attached drag targets to no longer be associated with this drop target
            dragTargets.forEach { it.dropTargets.remove(this) }
            dragTargets.clear()
        }

        fun onDispose() = detachFromDragTargets()
    }

    private val dropTargetStates = mutableListOf<DropTargetState>()

    @Composable
    private fun rememberDropTargetState(
        onDragTargetAdded: (T) -> Unit,
        onDragTargetRemoved: (T) -> Unit,
        options: DropOptions = DropOptions(),
        content: @Composable (DropTargetStatus) -> Unit
    ): DropTargetState {
        val dropTargetState =
            remember(onDragTargetAdded, onDragTargetRemoved, options, content) {
                DropTargetState(
                    onDragTargetAdded = onDragTargetAdded,
                    onDragTargetRemoved = onDragTargetRemoved,
                    options = options
                )
            }
        DisposableEffect(onDragTargetAdded, onDragTargetRemoved, options, content) {
            dropTargetStates.add(dropTargetState)
            onDispose {
                dropTargetState.onDispose()
                dropTargetStates.remove(dropTargetState)
            }
        }
        return dropTargetState
    }

    @Composable
    fun DropTarget(
        onDragTargetAdded: (T) -> Unit,
        // Not required because there may be no need to respond to re-drag events
        onDragTargetRemoved: (T) -> Unit = {},
        options: DropOptions = DropOptions(),
        content: @Composable (DropTargetStatus) -> Unit
    ) {
        val dropTargetState =
            rememberDropTargetState(onDragTargetAdded, onDragTargetRemoved, options, content)
        Box(
            modifier = Modifier.onGloballyPositioned {
                dropTargetState.globalRect = it.boundsInWindow()
            }
        ) {
            content(dropTargetState.status.value)
        }
    }
}

@Composable
fun <T> withDragContext(context: DragContext<T>, body: @Composable DragContext<T>.() -> Unit) {
    context.body()
}