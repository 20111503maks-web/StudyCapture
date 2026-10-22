package app.studycapture.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.studycapture.ui.cards.CardSuggestionsScreen
import app.studycapture.ui.check.CheckTextScreen
import app.studycapture.ui.check.PasteTextScreen
import app.studycapture.ui.home.HomeScreen
import app.studycapture.ui.library.LibraryScreen
import app.studycapture.ui.library.SubjectScreen
import app.studycapture.ui.material.MaterialScreen
import app.studycapture.ui.processing.ProcessingScreen
import app.studycapture.ui.profile.ProfileScreen
import app.studycapture.ui.review.ReviewScreen

@Composable
fun StudyNavHost(
    navController: NavHostController,
    onCapture: () -> Unit,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val back: () -> Unit = { navController.popBackStack() }
    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
        enterTransition = { fadeIn() + slideInHorizontally { it / 12 } },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() + slideOutHorizontally { it / 12 } },
    ) {
        composable<HomeRoute> {
            HomeScreen(
                onCapture = onCapture,
                onOpenMaterial = { navController.navigate(MaterialRoute(it)) },
                onContinue = { navController.navigate(CheckTextRoute(it)) },
                onProcessing = { navController.navigate(ProcessingRoute(it)) },
                onReview = { topicId -> navController.navigate(ReviewRoute(topicId)) },
                onSearch = { navController.navigate(LibraryRoute) },
            )
        }
        composable<LibraryRoute> {
            LibraryScreen(
                onOpenSubject = { navController.navigate(SubjectRoute(it)) },
                onOpenMaterial = { id, pageId, block -> navController.navigate(MaterialRoute(id, pageId, block)) },
                onOpenUnfinished = { id, processing ->
                    navController.navigate(if (processing) ProcessingRoute(id) else CheckTextRoute(id))
                },
            )
        }
        composable<SubjectRoute> { entry ->
            SubjectScreen(
                subjectId = entry.toRoute<SubjectRoute>().subjectId,
                onBack = back,
                onOpenMaterial = { navController.navigate(MaterialRoute(it)) },
                onReviewTopic = { navController.navigate(ReviewRoute(it)) },
            )
        }
        composable<ReviewRoute> { entry ->
            ReviewScreen(
                topicId = entry.toRoute<ReviewRoute>().topicId,
                onOpenSource = { id, pageId, block -> navController.navigate(MaterialRoute(id, pageId, block)) },
                onDone = { navController.navigate(HomeRoute) { popUpTo(HomeRoute) { inclusive = true } } },
            )
        }
        composable<ProfileRoute> { ProfileScreen(snackbar = snackbar) }
        composable<PasteTextRoute> {
            PasteTextScreen(
                onBack = back,
                onCreated = { id ->
                    navController.navigate(CheckTextRoute(id)) { popUpTo(PasteTextRoute) { inclusive = true } }
                },
            )
        }
        composable<ProcessingRoute> { entry ->
            val id = entry.toRoute<ProcessingRoute>().materialId
            ProcessingScreen(
                materialId = id,
                onBack = back,
                onReady = {
                    navController.navigate(CheckTextRoute(id)) { popUpTo<ProcessingRoute> { inclusive = true } }
                },
            )
        }
        composable<CheckTextRoute> { entry ->
            CheckTextScreen(
                materialId = entry.toRoute<CheckTextRoute>().materialId,
                onBack = back,
                onSaved = { id ->
                    navController.navigate(MaterialRoute(id)) { popUpTo(HomeRoute) }
                },
                onDeleted = { navController.popBackStack(HomeRoute, inclusive = false) },
            )
        }
        composable<MaterialRoute> { entry ->
            val route = entry.toRoute<MaterialRoute>()
            MaterialScreen(
                materialId = route.materialId,
                focusPageId = route.pageId,
                focusBlockIndex = route.blockIndex,
                onBack = back,
                onMakeCards = { navController.navigate(CardSuggestionsRoute(route.materialId)) },
                onEditText = { navController.navigate(CheckTextRoute(route.materialId)) },
                onDeleted = { navController.popBackStack() },
            )
        }
        composable<CardSuggestionsRoute> { entry ->
            val id = entry.toRoute<CardSuggestionsRoute>().materialId
            CardSuggestionsScreen(
                materialId = id,
                onBack = back,
                onOpenSource = { pageId, block -> navController.navigate(MaterialRoute(id, pageId, block)) },
                onStartReview = { navController.navigate(ReviewRoute()) { popUpTo(HomeRoute) } },
            )
        }
    }
}
