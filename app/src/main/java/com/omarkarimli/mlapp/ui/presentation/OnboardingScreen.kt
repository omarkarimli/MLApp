package com.omarkarimli.mlapp.ui.presentation

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import com.omarkarimli.mlapp.R
import com.omarkarimli.mlapp.ui.theme.MLAppTheme
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.utils.Dimens
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun OnboardingScreen(navController: NavHostController) {

    Scaffold { innerPadding ->
        ScrollContent(innerPadding, navController)
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingPreview() {
    MLAppTheme {
        OnboardingScreen(navController = NavHostController(LocalContext.current))
    }
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues,
    navController: NavHostController
) {
    val onboardingPages = listOf(
        OnboardingPageData(R.drawable.onboarding_image_1, "Welcome to MyCollab!", "Share and collaborate with friends and family."),
        OnboardingPageData(R.drawable.onboarding_image_2, "Collaborate with ease.", "Easily share, collab with friends & family."),
         OnboardingPageData(R.drawable.onboarding_image_3, "Stay organized.", "Use AI to detect contents, workflows.")
    )

    val pagerState = rememberPagerState(pageCount = {
        onboardingPages.size
    })

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Use Dimens for vertical spacing
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
                        .padding(horizontal = Dimens.PaddingMedium) // Use Dimens for horizontal padding
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightExtraLarge)) // Use Dimens for top spacer
                    Text(
                        text = onboardingPages[page].title,
                        style = MaterialTheme.typography.displayMedium,
                        lineHeight = Dimens.LineHeightDisplayMedium
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightMedium)) // Use Dimens for spacer
                    Image(
                        painter = painterResource(id = onboardingPages[page].imageResId),
                        contentDescription = onboardingPages[page].title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimens.OnboardingImageHeight) // Use Dimens for image height
                            .clip(RoundedCornerShape(Dimens.CornerRadiusLarge)) // Use Dimens for corner radius
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightLarge)) // Use Dimens for spacer
                    Text(
                        text = onboardingPages[page].description,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Normal,
                        lineHeight = Dimens.LineHeightHeadlineMedium, // Use Dimens for line height
                        modifier = Modifier.padding(end = Dimens.PaddingMedium) // Use Dimens for padding
                    )
                    Spacer(modifier = Modifier.height(Dimens.SpacerHeightLarge)) // Use Dimens for spacer
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
    onboardingPages: List<OnboardingPageData>
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
                .padding(horizontal = Dimens.PaddingMedium) // Use Dimens for horizontal padding
                .height(Dimens.ButtonHeight), // Use Dimens for button height
            shape = RoundedCornerShape(Dimens.CornerRadiusLarge), // Use Dimens for corner radius
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.onSurface,
                contentColor = MaterialTheme.colorScheme.surface
            )
        ) {
            val text = if (isLastPage) "Get Started" else "Continue"
            Text(
                text = text.uppercase(Locale.ROOT),
                letterSpacing = Dimens.LetterSpacingButton, // Use Dimens for letter spacing
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(Dimens.PaddingMedium)) // Use Dimens for spacer
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
            .padding(Dimens.PaddingMedium) // Use Dimens for padding
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
                    .padding(Dimens.DotIndicatorPadding) // Use Dimens for padding
                    .clip(CircleShape)
                    .background(color)
                    .height(Dimens.DotIndicatorSize) // Use Dimens for dot height
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

data class OnboardingPageData(
    val imageResId: Int,
    val title: String,
    val description: String
)