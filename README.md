MedScan
MedScan is an AI-powered Android application designed to enhance medication scanning, management, and medical consultation through intelligent OCR, real-time data retrieval, AI chatbot interaction, and geolocation-based healthcare provider lookup. The app targets multilingual healthcare accessibility, particularly for users in Morocco.

📱 Features
🔎 Medication Scanning with AI
Scan medication packaging using device camera.

OCR via Google ML Kit extracts text.

Google Gemini AI filters and enhances recognition accuracy.

Automatic translation from English to French using Google Translate API.

💊 Comprehensive Medication Information
Retrieves drug details from:

Local SQLite cache.

OpenFDA API via a Spring Boot backend.

Displays:

Indications, precautions, adverse effects, overdosage risks, and more.

Saves data locally for offline access.

🤖 AI-Powered Doctor Chatbot
Interactive health Q&A powered by Google Gemini AI.

Maintains full conversation history.

Provides general advice and medication insights.

🏥 Healthcare Provider Locator
Shows nearby specialists using Google Maps API.

Filters doctors by specialty.

Scrapes data from doctori.ma using JSOUP.

Displays contact info, address, and links to detailed profiles.![Uploading image.png…]()


⏰ Medication Reminders
Users set reminders with:

Medication name, dosage, and time.

Notes like “Take after meals”.

Repeat settings by day of the week.

🔄 Data Synchronization
Seamless sync between local SQLite and remote PostgreSQL database.

Periodic updates for medication records and providers.

🛠 Architecture
➤ Client: Android App (Java)
Built in Android Studio.

UI/UX, camera, geolocation, local database.

Communicates with backend via Retrofit.

➤ Server: Spring Boot Backend
Developed with Maven, Spring Data JPA.

Handles API requests and connects to:

PostgreSQL (medication catalog).

OpenFDA API (external data).

➤ Local & Remote Databases
SQLite for offline cache.

PostgreSQL for cloud-based persistent storage.

➤ External Services
OpenFDA API – Drug data.

Google Gemini AI API – OCR text filtering & chatbot.

Google Maps API – Location services.

JSOUP – Web scraping for doctor listings.

📦 Technologies Used

Component	Technology
OCR	Google ML Kit
AI Integration	Google Gemini AI
API Communication	Retrofit
JSON Parsing	Gson
Local Storage	SQLite
Backend Framework	Spring Boot
DB Management	PostgreSQL, SQLite
Geolocation	Google Maps API
Web Scraping	JSoup
Data Layer	Spring Data JPA
Translation	Google Translate API

📖 Usage Examples
✅ Scanning a Medication
User scans "Aspirin" → OCR detects name → If not cached, fetches from OpenFDA → Displays detailed info → User can translate to French.

✅ Chatbot Consultation
Asks: "Can I take Aspirin with blood pressure meds?" → Gemini AI explains risks and advises seeing a doctor.

✅ Locate a Cardiologist
User selects "Cardiologist" → App shows nearby specialists on map → Taps marker → Sees scraped data + external link to profile.

✅ Set Medication Reminders
Configures Amoxicillin (500mg) → Alarms at 8 AM & 8 PM → Notes: "Take after meals" → Repeats daily.

🌐 Target Audience
French & English speakers in Morocco.

Users seeking:

Medication insight

AI-driven health consultation

Nearby doctor discovery

🧠 Development Guidelines
Follow MVC Architecture:

View: Android UI

Controller: Retrofit, Gemini AI integration

Model: SQLite, backend entities

Maintain modular code for OCR, API, geolocation, chatbot, and scraping logic.

🚀 Future Enhancements
Secure authentication (OAuth2 / Firebase Auth)

Cross-platform version (iOS / Web)

Patient medical records sync

Appointment scheduling integration with doctori.ma

Voice-enabled chatbot interaction

👥 Contributors
Wadii

Alichan Driss








