# Summary
The aim of this library is to provide utilities that simplify gesture-based interactions, starting with easily configured drag-and-drop functionality.

# Platforms
This library is compatible with Compose Multiplatform and currently supports the iOS and Android targets.

# Drag-and-drop with `DragContext`
## Motivation
In Jetpack Compose, gesture-based behavior is added to composables via [Compose modifiers](https://developer.android.com/jetpack/compose/modifiers). For instance, the [`Modifier.draggable` modifier](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.Modifier).draggable(androidx.compose.foundation.gestures.DraggableState,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Boolean,kotlin.coroutines.SuspendFunction2,kotlin.coroutines.SuspendFunction2,kotlin.Boolean)) can be used to detect when a composable is being dragged in a single orientation. To handle more complex cases of dragging composables, the [`Modifier.pointerInput` modifier](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/pointer/package-summary#(androidx.compose.ui.Modifier).pointerInput(kotlin.Any,kotlin.coroutines.SuspendFunction1)) is available.

In both of these cases, detection of user input is handled separately from actually moving the dragged composable on the screen; to move the composable, the detected offset must be passed to [`Modifier.offset`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).offset(androidx.compose.ui.unit.Dp,androidx.compose.ui.unit.Dp)). On top of this, there is no default concept of drag-and-drop where a receiving composable (a "drop target") can be configured to receive data from the dragged composable (a "drag target"), nor is there any built-in concept of the current state of the drag or drop targets (e.g. being hovered over).

The `DragContext` API was created to enable flexible drag-and-drop behavior while encapsulating the complexity of the implementation details so that developers can focus on core app logic instead. Phrased differently, the `DragContext` API is meant to be a declarative way to specify drag-and-drop behavior — to specify _what_ you want to be dragged and dropped without needing to specify _how_.

## Reference
TODO

## Usage
TODO

## Demos
https://github.com/susumunoda/compose-gestures/assets/5313576/40cf6f7d-f881-4e8b-bb8e-b23a19e7ee56

See the [source code](https://github.com/susumunoda/compose-gestures/blob/main/demo-app/src/main/java/com/susumunoda/compose/gestures/demo/CoinJars.kt).

For a more complex example, see [susumunoda/word-game](https://github.com/susumunoda/word-game).

## Future
It is worth noting that the `DragContext` API does not currently take advantage of the experimental [`AnchoredDraggable`](https://developer.android.com/jetpack/compose/touch-input/pointer-input/migrate-swipeable) API. It is possible that as that API matures, the `DragContext` API may also change or possibly no longer be necessary.
