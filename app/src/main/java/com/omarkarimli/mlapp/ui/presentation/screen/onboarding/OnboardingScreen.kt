package com.omarkarimli.mlapp.ui.presentation.screen.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.domain.models.OnboardingPageModel
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun OnboardingScreen() {
    Scaffold { innerPadding ->
        ScrollContent(innerPadding)
    }
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues
) {
    val navController = LocalNavController.current

    val onboardingPages = listOf(
        OnboardingPageModel(R.drawable.i1, "Welcome to ${stringResource(id = R.string.app_name)}!", "Intelligent tools for your daily tasks."),
        OnboardingPageModel(R.drawable.i2, "Scan and understand.", "Instantly recognize text, objects, and more."),
        OnboardingPageModel(R.drawable.i3, "Stay smarter.", "AI-powered tags to keep everything in its place.")
    )

    val pagerState = rememberPagerState(pageCount = {
        onboardingPages.size
    })

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding() + Dimens.PaddingSmall))
        DotsIndicator(pagerState)
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = Dimens.PaddingMedium)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightExtraLarge))
                    Text(
                        text = onboardingPages[page].title,
                        style = MaterialTheme.typography.displayMedium,
                        lineHeight = Dimens.LineHeightDisplayMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightMedium))
                    Image(
                        painter = painterResource(id = onboardingPages[page].imageResId),
                        contentDescription = onboardingPages[page].title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.OnboardingImageHeight)
                            .clip(RoundedCornerShape(Dimens.CornerRadiusLarge))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightLarge))
                    Text(
                        text = onboardingPages[page].description,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal,
                        lineHeight = Dimens.LineHeightHeadlineMedium,
                        modifier = Modifier.padding(end = Dimens.PaddingMedium)
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightLarge))
                }
            }

            BottomButton(
                state = pagerState,
                navController = navController,
                onboardingPages = onboardingPages
            )
        }
    }
}

@Composable
fun BottomButton(
    state: PagerState,
    navController: NavHostController,
    onboardingPages: List<OnboardingPageModel>
) {
    val isLastPage = (state.currentPage == onboardingPages.size - 1)
    val coroutineScope = rememberCoroutineScope()
    val onButtonClick = if (isLastPage) {
        {
            navController.navigate(Screen.Login.route)
        }
    } else {
        {
            coroutineScope.launch {
                state.animateScrollToPage(state.currentPage + 1)
            }
        }
    }

    Column {
        Button(
            onClick = { onButtonClick.invoke() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.PaddingMedium)
                .height(Dimens.ButtonHeight),
            shape = RoundedCornerShape(Dimens.CornerRadiusLarge),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            val text = if (isLastPage) "Get Started" else "Continue"
            Text(
                text = text.uppercase(Locale.ROOT),
                letterSpacing = Dimens.LetterSpacingButton,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(Dimens.PaddingMedium))
    }
}

@Composable
fun DotsIndicator(pagerState: PagerState) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(Dimens.PaddingMedium)
    ) {
        repeat(pagerState.pageCount) { iteration ->
            val isCurrentPage = pagerState.currentPage == iteration
            val color by animateColorAsState(
                targetValue = if (isCurrentPage) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                },
                animationSpec = tween(durationMillis = 300)
            )

            Box(
                modifier = Modifier
                    .padding(Dimens.DotIndicatorPadding)
                    .clip(CircleShape)
                    .background(color)
                    .height(Dimens.DotIndicatorSize)
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}