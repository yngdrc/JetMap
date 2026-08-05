package app.aventurine.jetmapdemo.ui.modules.main

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.aventurine.jetmap.controller.JetMapController
import app.aventurine.jetmap.controller.marker.models.MarkerDescriptor
import app.aventurine.jetmap.controller.navigation.NavigationState
import app.aventurine.jetmap.domain.models.MapConfigEntity
import app.aventurine.jetmap.domain.models.MarkerEntity
import app.aventurine.jetmap.domain.repositories.MarkerRepository
import app.aventurine.jetmap.ui.JetMapConfig
import app.aventurine.jetmapdemo.R
import app.aventurine.jetmapdemo.ui.modules.main.providers.MapProvidersFactory
import app.aventurine.jetmapdemo.ui.modules.main.states.MainBottomSheetUIState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class MainViewModel @Inject constructor(
    private val markerRepository: MarkerRepository,
    providersFactory: MapProvidersFactory,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mapConfig: MapConfigEntity? = savedStateHandle[MAP_CONFIG_KEY]

    private val _errorStateFlow: MutableStateFlow<String?> = MutableStateFlow(value = null)
    val errorStateFlow: StateFlow<String?> = _errorStateFlow.asStateFlow()

    private val providers = mapConfig?.let(providersFactory::create)

    val jetMapController: JetMapController? = mapConfig?.let { config ->
        providers?.let { mapProviders ->
            JetMapController(
                parentScope = viewModelScope,
                tileProvider = mapProviders.tileProvider,
                markerProvider = mapProviders.markerProvider,
                pathProvider = mapProviders.pathProvider,
                config = JetMapConfig(
                    tileSize = config.tileSize,
                    mapSize = IntSize(width = config.width, height = config.height),
                    initialLevel = config.baseFloor
                )
            )
        }
    }

    private val _bottomSheetUIStateFlow: MutableStateFlow<MainBottomSheetUIState> =
        MutableStateFlow(value = MainBottomSheetUIState.Initial)

    val bottomSheetUIStateFlow: StateFlow<MainBottomSheetUIState> =
        _bottomSheetUIStateFlow.asStateFlow()

    val navigationStateFlow: StateFlow<NavigationState> =
        jetMapController?.navigationApi?.navigationStateFlow
            ?: MutableStateFlow(NavigationState.Idle)

    private val _queryStateFlow: MutableStateFlow<String> = MutableStateFlow(value = "")
    val queryStateFlow: StateFlow<String> = _queryStateFlow.asStateFlow()
    fun onQueryChange(query: String) = _queryStateFlow.update { query }

    /**
     * `flatMapLatest` cancels the in-flight query, `catch` keeps the flow alive when the
     * repository throws. Previously a single failure killed the whole search for good.
     */
    val searchResultsFlow: StateFlow<List<MarkerEntity>> = queryStateFlow
        .debounce(SEARCH_DEBOUNCE)
        .flatMapLatest { query ->
            flow {
                emit(
                    if (query.isBlank()) {
                        emptyList()
                    } else {
                        markerRepository.search(query = query)
                    }
                )
            }.catch { emit(emptyList()) }
        }
        .flowOn(context = Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT),
            initialValue = emptyList()
        )

    init {
        if (mapConfig == null) {
            _errorStateFlow.value = "Brak konfiguracji mapy"
        }

        observeFocusedMarker()
        observeMapTaps()
    }

    private fun observeFocusedMarker() {
        val controller = jetMapController ?: return

        viewModelScope.launch {
            controller.gestureApi.focusedMarkerFlow.collectLatest { markerDescriptor ->
                // Route planning is driven by [observeMapTaps], which also accepts empty space.
                if (_bottomSheetUIStateFlow.value is MainBottomSheetUIState.RoutePlanning) {
                    return@collectLatest
                }

                _bottomSheetUIStateFlow.value = markerDescriptor
                    ?.let(MainBottomSheetUIState::MarkerDetails)
                    ?: MainBottomSheetUIState.Initial
            }
        }
    }

    /**
     * While planning a route any point of the map can be used as the starting point, exactly like
     * before: tapping empty space drops an ad-hoc pin instead of being ignored.
     */
    private fun observeMapTaps() {
        val controller = jetMapController ?: return

        viewModelScope.launch {
            controller.gestureApi.tapResultFlow.collectLatest { tapResult ->
                val state = _bottomSheetUIStateFlow.value as? MainBottomSheetUIState.RoutePlanning
                    ?: return@collectLatest

                // Do not move the origin while turn-by-turn guidance is running.
                if (navigationStateFlow.value is NavigationState.Navigating) {
                    return@collectLatest
                }

                val origin = tapResult.marker ?: MarkerDescriptor(
                    x = tapResult.x,
                    y = tapResult.y,
                    z = tapResult.level,
                    iconId = R.drawable.ic_map_player,
                    description = "${tapResult.x}, ${tapResult.y}"
                )

                if (origin.id == state.destination.id) {
                    return@collectLatest
                }

                setOrigin(origin = origin)
            }
        }
    }

    fun onMarkerSelected(markerEntity: MarkerEntity) {
        jetMapController?.gestureApi?.changeFocusedMarker(
            focusedMarker = MarkerDescriptor(
                x = markerEntity.x,
                y = markerEntity.y,
                z = markerEntity.floor,
                iconId = null,
                description = markerEntity.description
            )
        )
    }

    /** "Nawiguj" on the marker details sheet. */
    fun startRoutePlanning() {
        val marker = (_bottomSheetUIStateFlow.value as? MainBottomSheetUIState.MarkerDetails)
            ?.markerDescriptor ?: return

        _bottomSheetUIStateFlow.value = MainBottomSheetUIState.RoutePlanning(
            origin = null,
            destination = marker
        )
    }

    fun setOrigin(origin: MarkerDescriptor) {
        val state = _bottomSheetUIStateFlow.value as? MainBottomSheetUIState.RoutePlanning ?: return
        _bottomSheetUIStateFlow.value = state.copy(origin = origin)
        requestRoute()
    }

    fun swapEndpoints() {
        val state = _bottomSheetUIStateFlow.value as? MainBottomSheetUIState.RoutePlanning ?: return
        val origin = state.origin ?: return

        _bottomSheetUIStateFlow.value = state.copy(
            origin = state.destination,
            destination = origin
        )
        requestRoute()
    }

    private fun requestRoute() {
        val state = _bottomSheetUIStateFlow.value as? MainBottomSheetUIState.RoutePlanning ?: return
        val origin = state.origin ?: return
        val controller = jetMapController ?: return

        if (origin.id == state.destination.id) {
            return
        }

        controller.navigationApi.findRoute(
            startingPoint = IntOffset(x = origin.x, y = origin.y) to origin.z,
            endingPoint = IntOffset(
                x = state.destination.x,
                y = state.destination.y
            ) to state.destination.z
        )
    }

    fun startNavigation() = jetMapController?.navigationApi?.startNavigation()

    fun stopNavigation() {
        jetMapController?.navigationApi?.stopNavigation()
        _bottomSheetUIStateFlow.value = MainBottomSheetUIState.Initial
        jetMapController?.gestureApi?.changeFocusedMarker(focusedMarker = null)
    }

    fun showRouteOverview() = jetMapController?.navigationApi?.showOverview()

    fun recenter() = jetMapController?.navigationApi?.recenter()

    fun focusStep(stepIndex: Int) = jetMapController?.navigationApi?.focusStep(stepIndex)

    fun setViewportPadding(left: Float, top: Float, right: Float, bottom: Float) {
        jetMapController?.navigationApi?.setViewportPadding(left, top, right, bottom)
    }

    /** Feeds the guidance engine with the current user position. */
    fun updateUserPosition(x: Float, y: Float, level: Int) {
        jetMapController?.navigationApi?.updatePosition(x = x, y = y, level = level)
    }

    fun closeBottomSheet() {
        jetMapController?.gestureApi?.changeFocusedMarker(focusedMarker = null)
        jetMapController?.navigationApi?.stopNavigation()
        _bottomSheetUIStateFlow.value = MainBottomSheetUIState.Initial
    }

    override fun onCleared() {
        jetMapController?.dispose()
        super.onCleared()
    }

    private companion object {
        const val MAP_CONFIG_KEY = "mapConfig"
        const val STOP_TIMEOUT = 5_000L
        val SEARCH_DEBOUNCE = 300.milliseconds
    }
}