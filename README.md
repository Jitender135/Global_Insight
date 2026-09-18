# Global Insights - Android News Application

**Global Insights** is a modern Android news application designed to deliver real-time news updates, personalized article feeds, and an enhanced reading experience. Built using **Java**, **Android SDK (API 35)**, **Material Design 3**, **Firebase**, and **Retrofit2**, the application offers features such as offline bookmarking, text-to-speech article reading, configurable layout modes, dark theme support, and user feedback mechanisms.

---

## Features

### Curated & Personalized News Feeds
- **Multi-Feed Browsing**: Seamlessly switch between **My Feed**, **All News**, **Trending**, **National**, **International**, **Local News**, and topic-specific categories.
- **Topic Categories**: Coverage across Politics, Business, Culture, Health, Sports, Technology, Nature, and Entertainment.
- **Personalized "My Feed"**: User interests are synchronized with Firebase to deliver a tailored news experience.

### AI-Powered Story Intelligence & Q&A
- **Uncover Story Facts**: Interactive bottom sheet (`AskStoryBottomSheetDialog`) enabling readers to ask deep, contextual questions about any news article.
- **Quick Tap Question Chips**: Dynamic contextual suggestion chips generated per story for one-tap insight discovery.
- **Grounded Fact Retrieval & Scoping**: Powered by a Python FastAPI backend integrated with Groq AI models (e.g., Qwen 3.8 27B, Groq Compound). Ensures responses stay strictly scoped to verified news context and domain background.
- **Story Source Citations**: Direct primary source attribution and tier badges attached to every response.
- **Smart Story Caching**: High-performance SHA-256 in-memory caching system (`StoryCacheManager`) preventing redundant API calls and guaranteeing instant responses.

### Dual Reading Modes
- **Standard Card View**: Clean list layout displaying news cards with thumbnails, timestamps, and source badges.
- **Inshorts-Style Mode**: Immersive full-screen cards designed for rapid news consumption.

### Text-to-Speech (TTS) Reader
- Listen to news article summaries using Android's native `TextToSpeech` engine.

### Bookmarks & Offline Access
- Save news articles locally for offline access via the `BookmarkManager` utility.

### Dark Mode & Dynamic Themes
- Comprehensive support for Light and Dark themes, with adaptive color palettes and icon sets.

### Firebase Authentication & User Profiles
- Secure user registration and login powered by **Firebase Authentication**.
- User profile data and interest preferences stored and managed in **Firebase Realtime Database**.

### Content Reporting & User Feedback
- Moderation system allowing users to report articles for fake news, clickbait, hate speech, or misinformation.
- Dedicated user support and feedback interfaces (`FeedbackActivity`, `ContactActivity`).

### Notification Preferences
- Configurable settings for Breaking News alerts, Daily Digest, and Topic Recommendations (`NotificationsActivity`).

### Caching & Performance
- 15-minute in-memory caching mechanism (`CACHE_DURATION_MS`) to minimize API requests and conserve network bandwidth.
- Smooth pull-to-refresh capabilities implemented via `SwipeRefreshLayout`.

### Multi-Language Support
- Application locale management with `LocaleHelper` for dynamic language switching.

---

## Project Structure

```
Global_Insight/
├── app/
│   ├── src/main/java/com/example/global_insights/
│   │   ├── Adapter/
│   │   │   ├── CategoryAdapter.java      # RecyclerView adapter for category chips
│   │   │   └── NewsAdapter.java          # Adapter for standard and inshorts view types
│   │   ├── model/
│   │   │   ├── Article.java              # Article data model
│   │   │   ├── AskStoryModel.java        # Q&A message, citation, and chat history model
│   │   │   ├── NewsModel.java            # News payload wrapper
│   │   │   ├── NewsResponse.java         # API response wrapper
│   │   │   ├── Report.java               # Content report data model
│   │   │   ├── Source.java               # Publication source model
│   │   │   ├── Story.java                # Grounded story domain model
│   │   │   └── User.java                 # User profile data model
│   │   ├── Network/
│   │   │   ├── ApiClient.java            # Retrofit client singleton
│   │   │   ├── GrokStoryService.java     # Android service connecting to FastAPI Story Intelligence API
│   │   │   └── NewsApiService.java       # Retrofit API interface
│   │   ├── AboutActivity.java            # Application information screen
│   │   ├── AskStoryBottomSheetDialog.java# Interactive AI Q&A modal dialog
│   │   ├── BookmarkManager.java          # Local bookmark persistence utility
│   │   ├── ContactActivity.java          # Contact support activity
│   │   ├── FeedbackActivity.java         # User feedback submission activity
│   │   ├── GlobalInsightsApp.java        # Application base class
│   │   ├── HomeActivity.java             # Main dashboard and navigation drawer
│   │   ├── InterestActivity.java         # Category preferences selection
│   │   ├── LocaleHelper.java             # Locale and language manager
│   │   ├── MainActivity.java             # App entry point launcher
│   │   ├── NotificationsActivity.java    # Notification preferences
│   │   ├── ReportNewsActivity.java       # News reporting dialog/activity
│   │   ├── SignInActivity.java           # Firebase login activity
│   │   └── SignUpActivity.java           # Firebase registration activity
│   └── src/main/res/
│       ├── layout/                       # UI layout definitions (including bottom_sheet_ask_story.xml)
│       ├── values/                       # Light theme styles and strings
│       └── values-night/                 # Dark theme overrides
├── backend/
│   ├── ai_service.py                     # Groq & Gemini AI model orchestration & prompt engine
│   ├── cache_manager.py                  # SHA-256 story query caching layer
│   ├── grok_service.py                   # Service interface wrapper
│   ├── main.py                           # FastAPI server entry point
│   └── requirements.txt                  # Python backend dependencies
├── build.gradle.kts                      # Root Gradle configuration
├── app/build.gradle.kts                  # App module Gradle configuration
└── settings.gradle.kts                   # Project settings and repository configuration
```

---

## Technical Stack & Dependencies

| Category | Technology / Library | Specification / Version |
| :--- | :--- | :--- |
| **Language** | Java 11 / Python 3.10+ | `JavaVersion.VERSION_11` |
| **Android SDK** | Min SDK 24 / Target SDK 35 | Android 7.0+ (Nougat and above) |
| **UI Design** | Material Components | `1.11.0` (Material Design 3) |
| **AI Intelligence** | FastAPI + Groq AI | Python 3.12, FastAPI 0.110+, Httpx, Groq API |
| **Networking** | Retrofit 2 & Gson Converter | `2.9.0` |
| **Image Loading** | Glide | `4.16.0` |
| **Backend & DB** | Firebase Auth & Realtime Database | `firebase-auth:22.3.0`, `firebase-database:20.3.0` |
| **UI Components** | SwipeRefreshLayout | `1.1.0` |

---

## Setup & Installation

### Prerequisites
- **Android Studio** (Ladybug / Jellyfish or newer).
- **JDK 11** installed and configured.
- **Python 3.10+** (for running the AI Story Intelligence FastAPI backend).
- Android device or emulator running **Android 7.0 (API level 24)** or higher.

### 1. Clone the Repository
```bash
git clone https://github.com/Jitender135/Global_Insight.git
cd Global_Insight
```

### 2. Start the FastAPI Story Intelligence Backend
1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install Python dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Create a `.env` file inside `backend/` and configure your Groq API Key:
   ```env
   GROQ_API_KEY=your_groq_api_key_here
   ```
4. Start the FastAPI uvicorn server:
   ```bash
   python main.py
   ```
   The backend runs on `http://0.0.0.0:8080` (accessible via `http://10.0.2.2:8080` on Android emulator).

### 3. Firebase Configuration
1. Open the [Firebase Console](https://console.firebase.google.com/) and create a new project.
2. Enable **Authentication** (Email/Password) and **Realtime Database**.
3. Register an Android application with package name `com.example.global_insights`.
4. Download the `google-services.json` file and place it in the `app/` directory (`app/google-services.json`).

### 4. API Key Configuration
1. Register for an API key at [NewsAPI.org](https://newsapi.org/).
2. Open [`HomeActivity.java`](app/src/main/java/com/example/global_insights/HomeActivity.java).
3. Set the `API_KEY` variable:
   ```java
   private final String API_KEY = "YOUR_NEWS_API_KEY";
   ```

### 5. Build and Run
1. Open the project in **Android Studio**.
2. Sync Gradle files (`File -> Sync Project with Gradle Files`).
3. Select your target device and click **Run** (`Shift + F10`).

---

## License

This project is licensed under the standard repository terms.

