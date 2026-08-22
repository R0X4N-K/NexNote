package io.github.r0x4nk.nexnote.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Shows a compact action after a list has moved far enough from its first item. */
@Composable
fun ScrollToTopButton(
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    val offsetThreshold = with(LocalDensity.current) { VISIBILITY_OFFSET.toPx().toInt() }
    val visible by remember(listState, offsetThreshold) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset >= offsetThreshold
        }
    }
    val scope = rememberCoroutineScope()
    ScrollToTopButton(
        visible = visible,
        onClick = { scope.launch { listState.animateScrollToItem(0) } },
        modifier = modifier
    )
}

/** Shows the same action for staggered note and template grids. */
@Composable
fun ScrollToTopButton(
    gridState: LazyStaggeredGridState,
    modifier: Modifier = Modifier
) {
    val offsetThreshold = with(LocalDensity.current) { VISIBILITY_OFFSET.toPx().toInt() }
    val visible by remember(gridState, offsetThreshold) {
        derivedStateOf {
            gridState.firstVisibleItemIndex > 0 ||
                gridState.firstVisibleItemScrollOffset >= offsetThreshold
        }
    }
    val scope = rememberCoroutineScope()
    ScrollToTopButton(
        visible = visible,
        onClick = { scope.launch { gridState.animateScrollToItem(0) } },
        modifier = modifier
    )
}

@Composable
private fun ScrollToTopButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        SmallFloatingActionButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll to top"
            )
        }
    }
}

private val VISIBILITY_OFFSET = 96.dp
