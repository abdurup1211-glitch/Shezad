package com.example.conversation

import java.util.Calendar

/**
 * Generates respectful day-time and night-time farewells for Sanju.
 */
class FarewellManager {

  fun buildFarewell(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String {
    val isNight = hourOfDay >= 19 || hourOfDay < 5
    return if (isNight) {
      "আল্লাহ হাফেজ সানজু।\nআপনার রাতটি শান্তিময় ও ভালো কাটুক।"
    } else {
      "আল্লাহ হাফেজ সানজু।\nআপনার দিনটি সুন্দর কাটুক।\nভালো থাকবেন।"
    }
  }
}
