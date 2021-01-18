package org.wordpress.android.ui.jetpack.scan.details

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.OpenThreatActionDialog
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.details.usecases.GetThreatModelUseCase
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

private const val ON_IGNORE_THREAT_BUTTON_CLICKED_PARAM_POSITION = 3
private const val TEST_SITE_NAME = "test site name"

@InternalCoroutinesApi
class ThreatDetailsViewModelTest : BaseUnitTest() {
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var getThreatModelUseCase: GetThreatModelUseCase
    @Mock private lateinit var ignoreThreatUseCase: IgnoreThreatUseCase
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var builder: ThreatDetailsListItemsBuilder
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var resourceProvider: ResourceProvider
    private lateinit var viewModel: ThreatDetailsViewModel
    private val threatId = 1L
    private val fakeUiStringText = UiStringText("")

    @Before
    fun setUp() = test {
        viewModel = ThreatDetailsViewModel(
            getThreatModelUseCase,
            ignoreThreatUseCase,
            selectedSiteRepository,
            builder,
            htmlMessageUtils,
            resourceProvider,
            TEST_DISPATCHER
        )
        whenever(site.name).thenReturn(TEST_SITE_NAME)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(any(), any())).thenReturn(mock())
        whenever(getThreatModelUseCase.get(anyLong())).thenReturn(mock())
        whenever(builder.buildThreatDetailsListItems(any(), any(), any(), any())).thenAnswer {
            createDummyThreatDetailsListItems(it.getArgument(ON_IGNORE_THREAT_BUTTON_CLICKED_PARAM_POSITION))
        }
    }

    @Test
    fun `given threat id, when on start, then threat details are retrieved`() = test {
        viewModel.start(threatId)

        verify(getThreatModelUseCase).get(threatId)
    }

    @Test
    fun `given threat id, when on start, then ui is updated with content`() = test {
        val uiStates = init().uiStates

        val uiState = uiStates.last()
        assertThat(uiState).isInstanceOf(Content::class.java)
    }

    @Test
    fun `when ignore threat button is clicked, then action to open ignore action confirmation dialog is triggered`() =
        test {
            val expectedDialogTitle = UiStringRes(R.string.threat_ignore)
            val expectedDialogMessage = UiStringText(
                htmlMessageUtils
                    .getHtmlMessageFromStringFormatResId(
                        R.string.threat_ignore_warning,
                        "<b>${site.name ?: resourceProvider.getString(R.string.scan_this_site)}</b>"
                    )
            )
            val expectedPositiveButtonLabel = R.string.dialog_button_ok
            val expectedNegativeButtonLabel = R.string.dialog_button_cancel
            val observers = init()

            (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>()
                .first().onClick.invoke()

            val confirmationDialogAction = observers.navigation.last().peekContent() as OpenThreatActionDialog
            with(confirmationDialogAction) {
                assertThat(title).isEqualTo(expectedDialogTitle)
                assertThat(message).isEqualTo(expectedDialogMessage)
                assertThat(positiveButtonLabel).isEqualTo(expectedPositiveButtonLabel)
                assertThat(negativeButtonLabel).isEqualTo(expectedNegativeButtonLabel)
            }
        }

    private fun createDummyThreatDetailsListItems(
        onIgnoreThreatItemClicked: () -> Unit
    ) = listOf(
        ActionButtonState(
            text = fakeUiStringText,
            contentDescription = fakeUiStringText,
            isSecondary = false,
            onClick = onIgnoreThreatItemClicked
        )
    )

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        val navigation = mutableListOf<Event<ThreatDetailsNavigationEvents>>()
        viewModel.navigationEvents.observeForever {
            navigation.add(it)
        }

        viewModel.start(threatId)

        return Observers(uiStates, navigation)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val navigation: List<Event<ThreatDetailsNavigationEvents>>
    )
}
