package com.omarkarimli.mlapp.ui.presentation.screen.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.omarkarimli.mlapp.ui.navigation.LocalNavController
import com.omarkarimli.mlapp.ui.navigation.Screen
import com.omarkarimli.mlapp.ui.presentation.common.widget.MyTextField
import com.omarkarimli.mlapp.utils.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
    val viewModel: ProfileViewModel = hiltViewModel()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    val bioText by viewModel.bioText.collectAsState()
    val websiteText by viewModel.websiteText.collectAsState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MyTopAppBar(scrollBehavior, onSave = { viewModel.saveSettings(bioText, websiteText) })
        }
    ) { innerPadding ->
        ScrollContent(innerPadding, bioText, websiteText)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTopAppBar(scrollBehavior: TopAppBarScrollBehavior, onSave: () -> Unit) {
    val navController = LocalNavController.current

    TopAppBar(
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            scrolledContainerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
        title = {
            Text(
                Screen.Profile.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(onClick = { navController.navigateUp() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            FilledTonalIconButton(
                onClick = onSave,
                modifier = Modifier
                    .width(Dimens.IconSizeExtraLarge)
                    .height(Dimens.IconSizeLarge),
                shape = IconButtonDefaults.filledShape
            ) {
                Icon(
                    Icons.Rounded.Done,
                    contentDescription = "Save",
                    modifier = Modifier.size(Dimens.IconSizeSmall),
                )
            }
            Spacer(Modifier.size(Dimens.PaddingSmall))
        }
    )
}

@Composable
private fun ScrollContent(
    innerPadding: PaddingValues,
    bioText: String,
    websiteText: String
) {
    Column(
        modifier = Modifier.padding(innerPadding).fillMaxWidth().padding(horizontal = Dimens.PaddingMedium)
    ) {
        Spacer(modifier = Modifier.height(Dimens.SpacerLarge))
        MyTextField("Bio", "Optional", bioText)
        Spacer(modifier = Modifier.height(Dimens.SpacerMedium))
        MyTextField("Website", "Optional", websiteText)
    }
}

