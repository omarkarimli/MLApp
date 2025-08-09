package com.omarkarimli.mlapp.ui.presentation.common.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun MyLottieAnimation(modifier: Modifier, resId: Int) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(resId))

    LottieAnimation(
        composition = composition,
        modifier = modifier,
        iterations = LottieConstants.IterateForever
    )
}