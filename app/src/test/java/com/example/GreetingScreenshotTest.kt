package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.sync.IncomingCallEvent
import com.example.ui.components.IncomingCallOverlay
import com.example.ui.theme.BharatChatTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun incoming_call_overlay_screenshot() {
    composeTestRule.setContent {
      BharatChatTheme(darkTheme = true) {
        IncomingCallOverlay(
          callEvent = IncomingCallEvent(
            callerName = "Vikram Aditya",
            callerAvatar = "VA",
            isVideo = false,
            callerPhone = "+91 98765 43210"
          ),
          onAccept = {},
          onDecline = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/incoming_call_overlay.png")
  }
}

