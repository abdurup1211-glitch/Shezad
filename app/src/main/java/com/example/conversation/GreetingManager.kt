package com.example.conversation

/**
 * Provides natural, varied greetings and busy-timeout responses for Sanju when "MAX" is called.
 */
class GreetingManager {
  private val wakeGreetings =
    listOf(
      "আসসালামু আলাইকুম, জি সানজু।\nকেমন আছেন?\nআজ কি কোনো কাজ আছে?",
      "আসসালামু আলাইকুম সানজু।\nসবকিছু কেমন চলছে?\nবলুন, আজ আপনাকে কীভাবে সাহায্য করতে পারি?",
      "ওয়ালাইকুম আসসালাম সানজু, আমি MAX।\nকেমন আছেন আপনি?\nকোনো কাজ থাকলে নির্দ্বিধায় বলুন।",
    )

  private var greetingCursor = -1

  fun nextWakeGreeting(): String {
    greetingCursor = (greetingCursor + 1) % wakeGreetings.size
    return wakeGreetings[greetingCursor]
  }

  fun primaryWakeGreeting(): String = wakeGreetings.first()

  fun noResponseTimeoutMessage(): String =
    "সানজু, মনে হচ্ছে আপনি একটু ব্যস্ত আছেন।\nঠিক আছে, যখন প্রয়োজন হবে শুধু MAX বলে ডাকবেন।\nআল্লাহ হাফেজ।"
}
