package com.app.weather.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun IOSWheelPicker(
    options: List<String>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val itemHeight = 40.dp
    val itemHeightPx = with(density) { itemHeight.toPx() }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val centerOffset = layoutInfo.viewportEndOffset / 2
            var closestIndex = selectedIndex
            var minDiff = Int.MAX_VALUE

            layoutInfo.visibleItemsInfo.forEach { item ->
                val itemCenter = item.offset + (item.size / 2)
                val diff = kotlin.math.abs(itemCenter - centerOffset)
                if (diff < minDiff) {
                    minDiff = diff
                    closestIndex = item.index
                }
            }
            
            if (closestIndex != selectedIndex && closestIndex in options.indices) {
                onIndexSelected(closestIndex)
            }
            listState.animateScrollToItem(closestIndex)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * 5)
            .fillMaxWidth()
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(options.size) { index ->
                val offsetFromCenter = remember(listState) {
                    derivedStateOf {
                        val layoutInfo = listState.layoutInfo
                        val viewPortCenter = layoutInfo.viewportEndOffset / 2f
                        val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                        if (itemInfo != null) {
                            val itemCenter = itemInfo.offset + itemInfo.size / 2f
                            (itemCenter - viewPortCenter) / (itemHeightPx * 2f) 
                        } else 1f
                    }
                }

                val normalizedOffset = offsetFromCenter.value.coerceIn(-1f, 1f)
                val rotationDeg = normalizedOffset * 60f 
                val alphaCalc = 1f - kotlin.math.abs(normalizedOffset)

                Box(
                    modifier = Modifier
                        .height(itemHeight)
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = alphaCalc.coerceIn(0.1f, 1f)
                            rotationX = rotationDeg
                            cameraDistance = 12f * density.density
                        }
                        .clickable {
                            coroutineScope.launch { listState.animateScrollToItem(index) }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = options[index],
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = if (kotlin.math.abs(normalizedOffset) < 0.2f) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // Fading edges for better look
        Box(modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.6f),
                0.2f to Color.Transparent,
                0.8f to Color.Transparent,
                1f to Color.Black.copy(alpha = 0.6f)
            )
        ))
    }
}
