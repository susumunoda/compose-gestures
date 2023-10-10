package com.susumunoda.compose.gestures.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.susumunoda.compose.gestures.DragContext
import com.susumunoda.compose.gestures.DragTargetStatus
import com.susumunoda.compose.gestures.DropOptions
import com.susumunoda.compose.gestures.withDragContext

private val dragContext = DragContext<Float>()

private enum class Coin(val value: Float, val size: Dp) {
    PENNY(.01f, 30.dp),
    NICKEL(.05f, 40.dp),
    DIME(.1f, 25.dp),
    QUARTER(.25f, 50.dp),
    HALF_DOLLAR(.5f, 60.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinJars() {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Coin Jars") })
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Row(
                // Add z-index of 1 so that coins appear over the jars
                modifier = Modifier
                    .zIndex(1f)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Coin(Coin.PENNY)
                Coin(Coin.NICKEL)
                Coin(Coin.DIME)
                Coin(Coin.QUARTER)
                Coin(Coin.HALF_DOLLAR)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Jar("Jar 1")
                Jar("Jar 2")
                Jar("Jar 3")
            }
            Button(onClick = { dragContext.resetDragTargets() }) {
                Text("Reset")
            }
        }
    }
}

@Composable
private fun Coin(coin: Coin, modifier: Modifier = Modifier) {
    withDragContext(dragContext) {
        DragTarget(data = coin.value) { dragTargetStatus ->
            Box(
                modifier = modifier
                    .size(coin.size)
                    .clip(CircleShape)
                    .alpha(if (dragTargetStatus == DragTargetStatus.DRAGGED) .5f else 1f)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text("${(coin.value * 100).toInt()}¢")
            }
        }
    }
}

@Composable
private fun Jar(
    label: String,
    modifier: Modifier = Modifier
) {
    var total by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        withDragContext(dragContext) {
            DropTarget(
                onDragTargetAdded = { total += it },
                onDragTargetRemoved = { total -= it },
                options = DropOptions(maxDragTargets = Int.MAX_VALUE)
            ) {
                Box(
                    Modifier
                        .width(80.dp)
                        .height(200.dp)
                        .background(Color.Yellow)
                )
            }
        }
        Text(label)
        Text(String.format("$%.2f", total))
    }
}
