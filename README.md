# Summary
The aim of this library is to provide utilities that simplify gesture-based interactions, starting with easily configured drag-and-drop functionality.

# Platforms
This library is compatible with Compose Multiplatform and currently supports the iOS and Android targets.

# Drag-and-drop with `DragContext`
## Motivation
In Jetpack Compose, gesture-based behavior is added to composables via [Compose modifiers](https://developer.android.com/jetpack/compose/modifiers). For instance, the [`Modifier.draggable` modifier](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/package-summary#(androidx.compose.ui.Modifier).draggable(androidx.compose.foundation.gestures.DraggableState,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Boolean,kotlin.coroutines.SuspendFunction2,kotlin.coroutines.SuspendFunction2,kotlin.Boolean)) can be used to detect when a composable is being dragged in a single orientation. To handle more complex cases of dragging composables, the [`Modifier.pointerInput` modifier](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/pointer/package-summary#(androidx.compose.ui.Modifier).pointerInput(kotlin.Any,kotlin.coroutines.SuspendFunction1)) is available.

In both of these cases, detection of user input is handled separately from actually moving the dragged composable on the screen; to move the composable, the detected offset must be passed to [`Modifier.offset`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier#(androidx.compose.ui.Modifier).offset(androidx.compose.ui.unit.Dp,androidx.compose.ui.unit.Dp)). On top of this, there is no default concept of drag-and-drop where a receiving composable (a "drop target") can be configured to receive data from the dragged composable (a "drag target"), nor is there any built-in concept of the current state of the drag or drop targets (e.g. being hovered over).

The `DragContext` API was created to enable flexible drag-and-drop behavior while encapsulating the complexity of the implementation details so that developers can focus on core app logic instead. Phrased differently, the `DragContext` API is meant to be a declarative way to specify drag-and-drop behavior — to specify _what_ you want to be dragged and dropped without needing to specify _how_.

## Usage
To use the `DragContext` API, first create an instance of `DragContext<T>` where `T` is the type parameter specifying the type of data that will be dropped into the drop target:
```kotlin
val dragContext = DragContext<Int>()
```

Next, specify a drag target by wrapping the desired composable in the `DragTarget` composable, which is a member function of the `dragContext` instance. For convenience, the `withDragContext` function is provided so that you can do:
```kotlin
withDragContext(dragContext) {
  DragTarget(data = ...) {
    // Your composable here
  }
}
```
instead of
```kotlin
dragContext.DragTarget(data = ...) { ... }
```

The next step is to specify a drop target by wrapping the desired composable in the `DropTarget` composable, similar to how `DragTarget` was configured:
```kotlin
withDragContext(dragContext) {
  DropTarget(onDragTargetAdded = { ... }) {
    // Your composable here
  }
}
```

**Note:** It is important that the same `DragContext` instance be used for both the `DragTarget` and `DropTarget`. This is because all of the internal state of the drag-and-drop items — e.g. the drag targets' positions, the drop targets' callbacks, etc. — are all contained internally in the `DragContext`.

**Tip:** While in this example the `dragContext` is locally accessible to both the `DragTarget` and `DropTarget` calls, this may not be the case in a larger, more complex application. In such cases, the `DragContext` instance can be provided to deeply nested areas of the application by creating a [`CompositionLocal`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal). For an example of this, see the [`susumunoda/word-game`](https://github.com/susumunoda/word-game) repository: [where the `CompositionLocal` is defined](https://github.com/susumunoda/word-game/blob/aa68377e2e025eeedeb8ab90b9cfc643338474fa/shared/src/commonMain/kotlin/com/susumunoda/wordgame/ui/screen/game/GameScreen.kt#L22), [where it is used to set up `DragTarget`s](https://github.com/susumunoda/word-game/blob/aa68377e2e025eeedeb8ab90b9cfc643338474fa/shared/src/commonMain/kotlin/com/susumunoda/wordgame/ui/screen/game/PlayerTilesSection.kt#L204), and [where it is used to set up `DropTarget`s](https://github.com/susumunoda/word-game/blob/aa68377e2e025eeedeb8ab90b9cfc643338474fa/shared/src/commonMain/kotlin/com/susumunoda/wordgame/ui/screen/game/GridSection.kt#L87).

## Reference
TODO

## Demos
https://github.com/susumunoda/compose-gestures/assets/5313576/40cf6f7d-f881-4e8b-bb8e-b23a19e7ee56

See the [source code](https://github.com/susumunoda/compose-gestures/blob/main/demo-app/src/main/java/com/susumunoda/compose/gestures/demo/CoinJars.kt).

For a more complex example, see [susumunoda/word-game](https://github.com/susumunoda/word-game).

## Future
It is worth noting that the `DragContext` API does not currently take advantage of the experimental [`AnchoredDraggable`](https://developer.android.com/jetpack/compose/touch-input/pointer-input/migrate-swipeable) API. It is possible that as that API matures, the `DragContext` API may also change or possibly no longer be necessary.
