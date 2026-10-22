package app.studycapture.ui.navigation

import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object LibraryRoute
@Serializable data class ReviewRoute(val topicId: String? = null)
@Serializable data object ProfileRoute
@Serializable data object PasteTextRoute
@Serializable data class SubjectRoute(val subjectId: String)
@Serializable data class ProcessingRoute(val materialId: String)
@Serializable data class CheckTextRoute(val materialId: String)
@Serializable data class MaterialRoute(val materialId: String, val pageId: String? = null, val blockIndex: Int? = null)
@Serializable data class CardSuggestionsRoute(val materialId: String)
