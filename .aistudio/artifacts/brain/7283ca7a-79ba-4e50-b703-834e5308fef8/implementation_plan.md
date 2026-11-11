# Lumi Mobile — Interactive Content Creator, Library Harvester & Offline H5P Studio

Lumi Mobile brings the desktop interactive learning power of Lumi (H5P ecosystem) to Android, featuring a native mobile creation studio for interactive activities, a pre-packaged and extensible H5P library manager, an automated library harvester from imported `.h5p` files, and a self-contained offline package generator and runner.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> **Key Revisions Incorporated from User Feedback**:
> 1. **Pre-Packaged Core H5P Libraries**: The app bundles essential H5P libraries in app assets (`assets/h5p/libraries/` & core runner scripts) for standard content types: **Quiz (Question Set - `H5P.QuestionSet 2.0`)**, Multiple Choice (`H5P.MultiChoice 1.14`), Flashcards/Dialog Cards (`H5P.Dialogcards 1.8`), Fill in the Blanks (`H5P.Blanks 1.12`), Single Choice Set, True/False, Drag the Words, Mark the Words, JoubelUI, and Question.
> 2. **Quiz (Question Set) Multi-Question Suite**: Implements `H5P.QuestionSet` allowing authors to sequence multiple MCQ questions into a single quiz activity with question progress dots, navigation (Previous/Next/Finish), per-question score checking, and a comprehensive final results screen with pass/retry badges and review mode.
> 3. **Bulk Question Import via CSV / Excel**: Authors can import large batches of questions at once using a standardized spreadsheet format (`Question, Correct Answer, Distractor 1, Distractor 2, Distractor 3, Feedback`), with support for file picker upload or pasting text directly from Excel/Sheets.
> 4. **Pre-Structured CSV Template Generator**: Built-in template generator that allows authors to download or share a pre-formatted `quiz_template.csv` with standard column headers and realistic educational examples.
> 5. **Option Position Randomization**: Shuffles answer choice positions for every question when displayed to learners (and reshuffles on quiz retries) so the correct answer never stays statically in position 1.
> 6. **Portable .h5p Export Pipeline**: Created activities can be exported into portable `.h5p` ZIP archives directly via the share sheet or saved directly to device storage using the Storage Access Framework (`CreateDocument`).

- **Confirmed Decision 1**: Pre-packaged core libraries with local dynamic expansion — Users never need an internet connection to author or play standard H5P types, and can expand available library types simply by loading any existing `.h5p` file.
- **Confirmed Decision 2**: Room Database & Library Registry — Library versions, dependencies, authored activities, and learner scores are indexed in Room for instantaneous querying and dependency verification.

---

### 1. Overview & Core Concept

- **What It Does**:
  - Author interactive educational activities (Multiple Choice, Flashcards, Fill-in-the-Blanks) directly on mobile devices.
  - Pre-packaged with standard H5P core runtime and libraries for instant offline creation.
  - Automatically harvests and updates H5P libraries from imported `.h5p` files when new content types are introduced.
  - Plays, tests, and validates `.h5p` packages offline with instant score feedback.
  - Exports standard `.h5p` ZIP files compatible with desktop Lumi and LMS platforms.
- **Target Audience / Persona**: Educators, instructional designers, trainers, and mobile learners seeking an offline-first H5P workshop on Android.
- **Key Value**: Eliminates dependency on desktop computers or online LMS servers for authoring, running, and managing H5P packages.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Activity Library & Dashboard (Home)**:
   - View pre-loaded interactive activities (quizzes, flashcard decks, interactive blanks) with badges indicating content type and library version.
   - Filter by content type, search by title, and inspect completion status and best scores.
   - One-tap "Play Offline", "Edit", or "Export .h5p".
2. **Interactive Player Screen**:
   - Renders the activity in a clean edge-to-edge container using the local offline runtime.
   - Full support for interactive controls, check answers, show solutions, retry, and score submission.
   - Score celebration modal with feedback and retry option.
3. **Mobile Activity Creator Studio**:
   - Select activity type (Multiple Choice Quiz, Flashcard Deck, Fill-in-the-Blanks).
   - Real-time dependency check against the Library Registry: If a required library is missing, offers a one-tap prompt to import any `.h5p` file containing that library.
   - Intuitive form fields for title, questions, answers, hints, and explanations.
   - Instant "Live Preview" tab switching between editor and player.
   - "Export as .h5p Package" button that bundles `h5p.json`, `content/content.json`, and all required library folders into a shareable `.h5p` file.
4. **H5P Library Manager & Harvester**:
   - List all installed libraries with their machine name, version (e.g., `H5P.MultiChoice 1.14.2`), author, and status (Bundled vs. Harvested).
   - "Harvest Libraries from .h5p" button: Select any `.h5p` file to scan, extract, and register all internal libraries.
   - "Check Dependencies" tool to verify which content types have complete library trees.

#### Visual Identity & Theme
- **Aesthetic Direction**: Sleek, professional educational studio. Clean typography, confident structure, and clear visual cues inspired by the Lumi Education brand.
- **Color Palette**:
  - *Primary*: Deep Teal / Indigo (`#1A5F7A` / `#2C74B3`) for navigation, buttons, and headers.
  - *Secondary / Accent*: Amber Gold (`#F2A93B`) for stars, scores, and active library pills.
  - *Surface / Background*: Soft Slate Tint (`#F8FAFC` light / `#0F172A` dark) with high-legibility cards.
  - *Status*: Emerald Green (`#10B981`) for satisfied dependencies, Amber (`#F59E0B`) for missing dependencies, Coral (`#EF4444`) for errors.
- **Typography & Hierarchy**: Bold display titles with clean, easily scannable body fonts.
- **Component Styling**: Floating Action Buttons for quick creation, segmented filter controls, and collapsible question cards with drag handles.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Pre-packaged Asset Bundles vs. Pure Remote Downloads**
  - *Chosen Approach*: Embed core H5P libraries directly in Android assets (`assets/h5p/`), and store additional harvested libraries in `filesDir/h5p_libraries/`.
  - *Why*: Ensures the app is 100% functional immediately after installation, with zero network setup or external API requirements.
- **Decision 2: Automated Library Harvesting from .h5p Files**
  - *Chosen Approach*: When any `.h5p` is loaded or imported, a background harvester inspects every folder matching `[libraryName]-[major].[minor]`, validates its `library.json`, and copies it to the app's persistent library repository.
  - *Why*: Users can easily introduce brand new H5P content types or update existing ones without an app update—simply by feeding any `.h5p` file containing the library.
- **Decision 3: Standards-Compliant .h5p Packager**
  - *Chosen Approach*: Implement a zip packaging service that packages the authored JSON data alongside the required library folders into a compliant `.h5p` file using Java `ZipOutputStream`.
  - *Why*: The exported files can be shared via Android's share sheet or opened in desktop Lumi, Canvas, Moodle, and WordPress without conversion.

---

### 4. Technical Architecture & Data Strategy (Technical Reference)

#### Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Lumi Mobile UI (Compose)                      │
│                                                                        │
│  ┌──────────────────┐  ┌──────────────────┐  ┌──────────────────────┐  │
│  │ Activity Library │  │  Creator Studio  │  │   Library Manager    │  │
│  │   & Dashboard    │  │ (Quizzes/Cards)  │  │  & Package Harvester │  │
│  └────────┬─────────┘  └────────┬─────────┘  └──────────┬───────────┘  │
└───────────┼─────────────────────┼───────────────────────┼──────────────┘
            │                     │                       │
┌───────────▼─────────────────────▼───────────────────────▼──────────────┐
│                            LumiViewModel                               │
│                                                                        │
│  • Library State & Activity Filtering                                  │
│  • Authoring State & Dependency Validation                             │
│  • Harvest Pipeline & Library Registry State                           │
│  • Interactive Player State & Score Callbacks                          │
└─────────────────────────────────┬──────────────────────────────────────┘
                                  │
┌─────────────────────────────────▼──────────────────────────────────────┐
│                            Repository Layer                            │
│                                                                        │
│  ┌───────────────────────────┐         ┌────────────────────────────┐  │
│  │    ActivityRepository     │         │      H5PLibraryRegistry    │  │
│  │   (Room DAO & Entities)   │         │ (Bundled Assets + Cache)   │  │
│  └─────────────┬─────────────┘         └─────────────┬──────────────┘  │
│                │                                     │                 │
│  ┌─────────────▼─────────────┐         ┌─────────────▼──────────────┐  │
│  │      H5PPackagerService   │         │    Offline Web Runner      │  │
│  │ (Zip Builder & Extractor) │         │ (Local AssetLoader + JS)   │  │
│  └───────────────────────────┘         └────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────┘
```

#### Data Model & Entities
- **`ActivityEntity`**:
  - `id: String` (UUID)
  - `title: String`
  - `description: String`
  - `mainLibrary: String` (e.g. `H5P.MultiChoice`, `H5P.Dialogcards`, `H5P.Blanks`)
  - `libraryVersion: String` (e.g. `1.14`)
  - `contentJson: String` (H5P content definition)
  - `h5pJson: String` (package manifest)
  - `isPreloaded: Boolean`
  - `bestScore: Int`
  - `totalQuestions: Int`
  - `updatedAt: Long`
- **`H5PLibraryEntity`**:
  - `machineName: String` (e.g. `H5P.MultiChoice`)
  - `majorVersion: Int`
  - `minorVersion: Int`
  - `patchVersion: Int`
  - `title: String`
  - `runnable: Boolean`
  - `isPrepackaged: Boolean`
  - `storagePath: String`
  - `dependenciesJson: String`
- **`AttemptEntity`**:
  - `id: Long` (Auto-increment)
  - `activityId: String`
  - `score: Int`
  - `maxScore: Int`
  - `completedAt: Long`

#### Core Services
1. **`H5PLibraryManager`**:
   - Manages pre-packaged libraries from `assets/h5p/libraries/`.
   - Manages harvested libraries stored in `filesDir/h5p_libraries/`.
   - Resolves dependency graphs (e.g. `H5P.MultiChoice` -> `H5P.Question`, `JoubelUI`, `FontAwesome`).
   - Reports missing dependencies and provides resolution suggestions.
2. **`H5PPackageEngine`**:
   - **Extractor/Harvester**: Unzips any `.h5p` archive, scans for `library.json` in directories, imports libraries into the registry, and parses `content/content.json`.
   - **Packager/Exporter**: Takes activity metadata, JSON, and copies the required library directories into a compliant `.h5p` ZIP file.
3. **`H5PPlayerWebView`**:
   - Hosts offline web runner using `WebViewAssetLoader`.
   - Seamlessly serves both pre-packaged assets and harvested libraries.
   - Provides two-way JavaScript bridge for scores, progress, and activity restart.
