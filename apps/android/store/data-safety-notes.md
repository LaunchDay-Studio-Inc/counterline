# Data Safety Notes — Google Play

## Data Safety Declaration

### Data Collection
**CounterLine does not collect or share any user data.**

### Detailed Responses for Play Console Data Safety Form

#### 1. Does your app collect or share any of the required user data types?
**No.**

#### 2. Is all of the user data collected by your app encrypted in transit?
Not applicable — the app makes no network requests.

#### 3. Do you provide a way for users to request that their data is deleted?
Users can clear all local data via Android Settings → Apps → CounterLine → Clear Data, or by uninstalling the app.

#### 4. Data types — for each category, answer:

| Data Type | Collected | Shared | Purpose |
|-----------|-----------|--------|---------|
| Location | No | No | — |
| Personal info (name, email, etc.) | No | No | — |
| Financial info | No | No | — |
| Health and fitness | No | No | — |
| Messages | No | No | — |
| Photos and videos | No | No | — |
| Audio files | No | No | — |
| Files and docs | No | No | — |
| Calendar | No | No | — |
| Contacts | No | No | — |
| App activity | No | No | — |
| Web browsing | No | No | — |
| App info and performance | No | No | — |
| Device or other IDs | No | No | — |

#### 5. Security practices
- All data stored on-device only
- No cloud backup of app data (backup is local SQLite only)
- Data at rest is protected by Android's app sandbox
- No third-party SDKs

### Notes for Reviewer
The app stores study progress (drill scores, spaced-repetition schedules) in a local Room/SQLite database and user preferences in Android DataStore. None of this data leaves the device. The app requests no permissions beyond basic storage for its private app directory (handled automatically by Android's scoped storage).
