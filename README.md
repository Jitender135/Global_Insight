# 🌐 Global Insights - Android News Application

[![Android](https://img.shields.io/badge/Platform-Android%20(API%2024%20--%2035)-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Java](https://img.shields.io/badge/Language-Java%2011-ED8B00?logo=openjdk&logoColor=white)](https://www.java.com/)
[![Python](https://img.shields.io/badge/Backend-FastAPI%20%7C%20Python%203.10%2B-009688?logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com/)
[![AI Engine](https://img.shields.io/badge/AI-Groq%20%2F%20Llama--3%20%7C%20Gemini-F55036)](https://groq.com/)
[![Firebase](https://img.shields.io/badge/Cloud-Firebase%20Auth%20%26%20Realtime%20DB-FFCA28?logo=firebase&logoColor=black)](https://firebase.google.com/)
[![UI Design](https://img.shields.io/badge/UI-Material%20Design%203-6750A4?logo=materialdesign&logoColor=white)](https://m3.material.io/)

**Global Insights** is a next-generation Android news application built to transcend traditional news aggregators. Combining **real-time 10 km radius hyper-local GPS reporting**, **Groq AI-powered Story Fact Verification**, **Inshorts-style vertical gesture navigation**, and **Firebase cloud synchronization**, Global Insights delivers verified, highly contextual news tailored to where you are and what you care about.

---

## 🌟 Key Value Proposition & Flagship Features

### 📍 1. Hyper-Local 10 km Radius News ("Around Me")
A unique feature that distinguishes Global Insights from conventional aggregators:
- **Live GPS Radius Detection**: Automatically captures your precise latitude and longitude using Google Play Services `FusedLocationProviderClient` and Android `Geocoder` to identify your exact sublocality, neighborhood, or campus (e.g., *Palam Vihar, Gurugram 122017*).
- **10 km Proximity Filter**: Fetches events, updates, and developments occurring strictly within a 10 km radius of your location.
- **Saved Places Management**:
  - Save key personal locations with custom emoji tags (🏠 Home, 🎓 College / Campus, 💼 Work, 📍 Custom Spot).
  - Rapid switcher bottom sheet (`SavedLocationsBottomSheetDialog`) to seamlessly toggle between your current Live GPS, Home, or University (e.g., *BML Munjal University, Kapriwas*).
  - Search any custom area or PIN code dynamically with instant geocoding.
- **Dedicated Backend RSS Engine**: Backend `/api/radius-news` endpoint streams real-time local updates with zero rate-limiting hurdles.

### 🧠 2. AI-Powered Story Intelligence & Fact Verification
- **Uncover Story Facts**: Interactive bottom sheet (`AskStoryBottomSheetDialog`) enabling readers to interrogate any news article in real time.
- **Grounded AI Retrieval**: Integrated with Groq AI high-speed inference (Llama-3 models) to ensure responses are strictly grounded in verified facts, filtering out sensationalism and clickbait.
- **Follow Ongoing Stories (Members Only)**:
  - Follow evolving news stories directly with one tap (`+ FOLLOW STORY` / `✓ FOLLOWING`).
  - Synced to **Firebase Realtime Database** under `users/{userId}/followed_stories/{storyKey}` with guest sign-in protection.
- **Source Attribution & Citations**: Explicit primary publisher attribution and credibility indicators on every answer.
- **SHA-256 Story Caching**: High-performance in-memory caching (`StoryCacheManager`) preventing redundant API calls and providing instantaneous responses.

### 📱 3. Dual Reading Modes & Modern UI
- **Immersive Inshorts-Style Vertical Mode**: Full-screen gesture-driven cards designed for rapid news consumption on the go.
- **Standard Card View**: Clean, scannable layout featuring high-resolution imagery, publisher badges, and publication timestamps.
- **Global Insights Brand Design System**: Refined aesthetic using deep royal blue (`#2563EB` / `#1D4ED8`), slate text hierarchy (`#0F172A` / `#64748B`), and subtle border treatments.

### 🔊 4. Text-to-Speech (TTS) & Localization
- Native Android `TextToSpeech` engine narrates full article summaries on demand.
- Multi-language support with dynamic locale switching via `LocaleHelper`.

### 💾 5. Offline Bookmarks & Personal Feeds
- **Offline Reading**: Save articles locally via `BookmarkManager` for reading without network connectivity.
- **Personalized "My Feed"**: User reading interests are synced with Firebase to tailor content recommendations.

### 🛡️ 6. Fact Moderation & Community Safety
- Article reporting system (`ReportNewsActivity`) empowering users to flag misinformation, hate speech, or clickbait.
- Integrated support and feedback channels (`FeedbackActivity`, `ContactActivity`).

---

## 🏛️ Architecture & Project Structure

```
Global_Insight/
├── app/
│   ├── src/main/java/com/example/global_insights/
│   │   ├── Adapter/
│   │   │   ├── CategoryAdapter.java             # Category filter chips
│   │   │   ├── NewsAdapter.java                 # Standard & Inshorts card adapter
│   │   │   └── SavedLocationsAdapter.java       # Saved places switcher adapter
│   │   ├── model/
│   │   │   ├── Article.java                     # Article data model
│   │   │   ├── AskStoryModel.java               # AI Q&A and citation model
│   │   │   ├── NewsModel.java                   # News payload container
│   │   │   ├── NewsResponse.java                # API response wrapper
│   │   │   ├── Report.java                      # Content moderation report model
│   │   │   ├── SavedLocation.java               # User saved location & coordinates model
│   │   │   ├── Source.java                      # News publisher model
│   │   │   ├── Story.java                       # Grounded story domain model
│   │   │   └── User.java                        # Firebase user profile model
│   │   ├── Network/
│   │   │   ├── ApiClient.java                   # Retrofit client singleton
│   │   │   ├── GrokStoryService.java            # AI Story Intelligence service
│   │   │   └── NewsApiService.java              # NewsAPI REST interface
│   │   ├── AboutActivity.java                   # About application screen
│   │   ├── AskStoryBottomSheetDialog.java       # AI Fact-Checking modal
│   │   ├── BookmarkManager.java                 # Local bookmark persistence
│   │   ├── ContactActivity.java                 # Developer contact screen
│   │   ├── FeedbackActivity.java                # User feedback interface
│   │   ├── FollowStoryManager.java              # Firebase story follow tracking
│   │   ├── GlobalInsightsApp.java               # Application context & init
│   │   ├── HomeActivity.java                    # Main feed & Around Me controller
│   │   ├── InterestActivity.java                # Category preferences screen
│   │   ├── LocaleHelper.java                    # Dynamic language switcher
│   │   ├── LocationHelper.java                  # GPS FusedLocation & Geocoding helper
│   │   ├── MainActivity.java                    # Splash & authentication router
│   │   ├── NotificationsActivity.java           # Push notification settings
│   │   ├── ReportNewsActivity.java              # Report abuse / fake news
│   │   ├── SavedLocationManager.java            # SharedPreferences saved places manager
│   │   ├── SavedLocationsBottomSheetDialog.java # Saved places bottom sheet dialog
│   │   ├── SignInActivity.java                  # Firebase authentication login
│   │   └── SignUpActivity.java                  # Firebase user registration
│   └── src/main/res/
│       ├── drawable/                            # UI drawables & vector assets
│       ├── layout/                              # Activity, fragment & dialog layouts
│       ├── values/                              # Colors, strings, themes
│       └── values-night/                        # Dark theme overrides
├── backend/
│   ├── ai_service.py                            # Groq & Gemini model orchestration
│   ├── cache_manager.py                         # SHA-256 in-memory caching engine
│   ├── grok_service.py                          # Service interface
│   ├── main.py                                  # FastAPI app (AI Q&A & Radius News)
│   ├── requirements.txt                         # Python dependencies
│   └── .env.example                             # Environment variable template
├── build.gradle.kts                             # Root build configuration
├── app/build.gradle.kts                         # App module dependencies & SDKs
└── settings.gradle.kts                          # Project settings
```

---

## 🛠️ Technology Stack

| Domain | Technology / Library | Version / Details |
| :--- | :--- | :--- |
| **Mobile Platform** | Android SDK | Min SDK 24 / Target SDK 35 (Android 15) |
| **Language** | Java 11 | `JavaVersion.VERSION_11` |
| **Location Services** | Google Play Services Location | `play-services-location:21.3.0` |
| **Backend Framework** | FastAPI (Python) | `fastapi>=0.110.0`, `uvicorn>=0.28.0` |
| **AI Inference** | Groq Cloud SDK | `groq>=0.9.0` (Llama-3, fast token generation) |
| **Networking** | Retrofit 2 & Gson | `retrofit:2.9.0`, `converter-gson:2.9.0` |
| **Image Loading** | Bumptech Glide | `glide:4.16.0` |
| **Authentication & DB** | Firebase Auth & Realtime DB | `firebase-auth:22.3.0`, `firebase-database:20.3.0` |
| **Design & Components** | Google Material Components | `material:1.11.0` (Material 3) |

---

## 🚀 Setup & Installation Guide

### Prerequisites
- **Android Studio** (Koala, Ladybug, or newer).
- **JDK 11** configured in your environment.
- **Python 3.10+** (for running the local AI & Radius News FastAPI backend).
- Android device or Android Virtual Device (AVD) running **Android 7.0 (API 24)** or higher with Google Play Services.

---

### 1. Clone the Repository
```bash
git clone https://github.com/Jitender135/Global_Insight.git
cd Global_Insight
```

---

### 2. Configure & Run Backend Services

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Create and activate a Python virtual environment (recommended):
   ```bash
   python -m venv venv
   # On Windows:
   venv\Scripts\activate
   # On macOS/Linux:
   source venv/bin/activate
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Configure environment variables:
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Edit `.env` to insert your Groq API key:
   ```env
   GROQ_API_KEY=your_groq_api_key_here
   ```
   > 🔒 **Security Note**: Never commit your `.env` file or private credentials to version control. The repository `.gitignore` automatically prevents `.env` and `local.properties` from being tracked.

5. Start the FastAPI server:
   ```bash
   python main.py
   ```
   The backend service starts at `http://0.0.0.0:8080`.
   - On the Android Emulator, this is accessed via `http://10.0.2.2:8080`.
   - On a physical Android device, use your host machine's local Wi-Fi IP address.

---

### 3. Firebase Setup
1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Authentication** (Email/Password provider).
3. Enable **Realtime Database** in test/production mode with appropriate security rules.
4. Register your Android app using package name:
   ```
   com.example.global_insights
   ```
5. Download your `google-services.json` and place it in the `app/` directory:
   ```
   Global_Insight/app/google-services.json
   ```

---

### 4. Build and Run the App
1. Open the project in **Android Studio**.
2. Perform a Gradle Sync (`File -> Sync Project with Gradle Files`).
3. Set your NewsAPI key in [`HomeActivity.java`](app/src/main/java/com/example/global_insights/HomeActivity.java) if querying external NewsAPI endpoints:
   ```java
   private final String API_KEY = "YOUR_NEWS_API_KEY";
   ```
4. Ensure Location permissions are granted when prompted to experience the **10 km Around Me** feature.
5. Launch the app on your emulator or connected device (`Shift + F10`).

---

## 🔒 Security & Privacy Best Practices

- **Zero Secret Commits**: All secret API keys, authentication credentials, and local environment paths (`.env`, `local.properties`, JVM paths) are excluded from git tracking.
- **Scoped AI Responses**: The AI Fact Verification backend enforces strict grounding prompt constraints, preventing hallucinations and restricting answers to the verified article scope.
- **Client-Side Location Privacy**: Coordinates acquired by `FusedLocationProviderClient` are utilized strictly for geocoding and news querying; locations are stored locally in private application preferences unless explicitly synchronized.

---

## 📄 License

This project is licensed under the MIT License - see the repository LICENSE file for details.
