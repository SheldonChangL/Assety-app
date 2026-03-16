package chang.sllj.homeassetkeeper.ui.camera

import android.view.MotionEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chang.sllj.homeassetkeeper.R
import chang.sllj.homeassetkeeper.camera.CameraCaptureResult
import chang.sllj.homeassetkeeper.camera.CameraEvent
import chang.sllj.homeassetkeeper.camera.CameraScanMode
import chang.sllj.homeassetkeeper.camera.CameraViewModel
import chang.sllj.homeassetkeeper.camera.NormalizedRect

/**
 * Full-screen camera viewfinder.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    scanMode: CameraScanMode,
    onCaptureResult: (CameraCaptureResult) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var focusPoint by remember { mutableStateOf<Offset?>(null) }

    // Consume one-time events.
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CameraEvent.CaptureSuccess -> onCaptureResult(event.result)
                is CameraEvent.ShowError      -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(scanMode.titleRes())) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor          = Color.Black.copy(alpha = 0.6f),
                    titleContentColor       = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── CameraX viewfinder ────────────────────────────────────────────
            val previewView = remember {
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                    isClickable = true
                }
            }

            AndroidView(
                factory = {
                    previewView.apply {
                        setOnTouchListener { _, event ->
                            if (event.action == MotionEvent.ACTION_UP) {
                                focusPoint = Offset(event.x, event.y)
                                viewModel.tapToFocus(
                                    x = event.x,
                                    y = event.y,
                                    meteringPointFactory = previewView.meteringPointFactory
                                )
                            }
                            true
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            LaunchedEffect(previewView) {
                viewModel.bindCamera(
                    lifecycleOwner  = lifecycleOwner,
                    surfaceProvider = previewView.surfaceProvider
                )
            }

            CameraGuideOverlay(scanMode = scanMode)
            FocusIndicatorOverlay(focusPoint = focusPoint)

            // ── Capturing overlay ─────────────────────────────────────────────
            if (uiState.isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                if (scanMode == CameraScanMode.PHOTO) {
                                    R.string.saving
                                } else {
                                    R.string.camera_processing_scan
                                }
                            ),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            // ── Shutter button ────────────────────────────────────────────────
            if (!uiState.isCapturing) {
                FloatingActionButton(
                    onClick = {
                        viewModel.captureImage(
                            scanMode = scanMode,
                            cropRect = scanMode.guideRectOrNull()
                        )
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        Icons.Filled.Camera,
                        contentDescription = stringResource(R.string.camera_capture_desc),
                        modifier           = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    LaunchedEffect(focusPoint) {
        if (focusPoint != null) {
            kotlinx.coroutines.delay(900)
            focusPoint = null
        }
    }
}

@Composable
private fun CameraGuideOverlay(scanMode: CameraScanMode) {
    val guideRect = scanMode.guideRectOrNull() ?: return
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerWidthPx = constraints.maxWidth.toFloat()
        val containerHeightPx = constraints.maxHeight.toFloat()
        val previewAspectRatio = 3f / 4f

        val previewWidthPx: Float
        val previewHeightPx: Float
        val previewLeftPx: Float
        val previewTopPx: Float

        if ((containerWidthPx / containerHeightPx) > previewAspectRatio) {
            previewHeightPx = containerHeightPx
            previewWidthPx = previewHeightPx * previewAspectRatio
            previewLeftPx = (containerWidthPx - previewWidthPx) / 2f
            previewTopPx = 0f
        } else {
            previewWidthPx = containerWidthPx
            previewHeightPx = previewWidthPx / previewAspectRatio
            previewLeftPx = 0f
            previewTopPx = (containerHeightPx - previewHeightPx) / 2f
        }

        val frameLeftPx = previewLeftPx + (previewWidthPx * guideRect.left)
        val frameTopPx = previewTopPx + (previewHeightPx * guideRect.top)
        val frameWidthPx = previewWidthPx * guideRect.widthFraction
        val frameHeightPx = previewHeightPx * guideRect.heightFraction
        val frameCornerPx = with(density) { 20.dp.toPx() }
        val instructionWidthDp = with(density) { frameWidthPx.toDp() }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f)
        ) {
            drawRect(Color.Black.copy(alpha = 0.45f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(frameLeftPx, frameTopPx),
                size = Size(frameWidthPx, frameHeightPx),
                cornerRadius = CornerRadius(frameCornerPx, frameCornerPx),
                blendMode = BlendMode.Clear
            )
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(frameLeftPx, frameTopPx),
                size = Size(frameWidthPx, frameHeightPx),
                cornerRadius = CornerRadius(frameCornerPx, frameCornerPx),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(scanMode.guideTitleRes()),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(scanMode.guideHintRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Card(
            modifier = Modifier
                .offset(
                    x = with(density) { frameLeftPx.toDp() },
                    y = with(density) { (frameTopPx + frameHeightPx + 12.dp.toPx()).toDp() }
                )
                .width(instructionWidthDp)
        ) {
            Text(
                text = stringResource(scanMode.guideSubHintRes()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FocusIndicatorOverlay(focusPoint: Offset?) {
    if (focusPoint == null) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        val radius = 36.dp.toPx()
        drawCircle(
            color = Color(0xFFFFD54F),
            radius = radius,
            center = focusPoint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx())
        )
    }
}

private fun CameraScanMode.titleRes(): Int = when (this) {
    CameraScanMode.PHOTO -> R.string.camera_title
    CameraScanMode.BRAND -> R.string.camera_title_scan_brand
    CameraScanMode.PURCHASE_DATE -> R.string.camera_title_scan_purchase_date
}

private fun CameraScanMode.guideRectOrNull(): NormalizedRect? = when (this) {
    CameraScanMode.PHOTO -> null
    CameraScanMode.BRAND -> NormalizedRect(
        left = 0.11f,
        top = 0.24f,
        right = 0.89f,
        bottom = 0.46f
    )
    CameraScanMode.PURCHASE_DATE -> NormalizedRect(
        left = 0.10f,
        top = 0.30f,
        right = 0.90f,
        bottom = 0.44f
    )
}

private fun CameraScanMode.guideTitleRes(): Int = when (this) {
    CameraScanMode.PHOTO -> R.string.camera_title
    CameraScanMode.BRAND -> R.string.camera_scan_brand_title
    CameraScanMode.PURCHASE_DATE -> R.string.camera_scan_purchase_date_title
}

private fun CameraScanMode.guideHintRes(): Int = when (this) {
    CameraScanMode.PHOTO -> R.string.camera_title
    CameraScanMode.BRAND -> R.string.camera_scan_brand_hint
    CameraScanMode.PURCHASE_DATE -> R.string.camera_scan_purchase_date_hint
}

private fun CameraScanMode.guideSubHintRes(): Int = when (this) {
    CameraScanMode.PHOTO -> R.string.camera_title
    CameraScanMode.BRAND -> R.string.camera_scan_brand_subhint
    CameraScanMode.PURCHASE_DATE -> R.string.camera_scan_purchase_date_subhint
}
