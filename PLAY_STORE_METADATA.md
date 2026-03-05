# Play Store Metadata — Home Asset Keeper

---

## English

### App Name *(max 30 chars)*

```
Home Asset Keeper
```
*(17 characters)*

---

### Short Description *(max 80 chars)*

```
Track your home assets, warranties & maintenance — 100% offline, zero cloud.
```
*(77 characters)*

---

### Full Description *(max 4000 chars)*

```
Home Asset Keeper is your private, offline-first home inventory manager.
Photograph, catalog, and track every item in your home — from appliances and
electronics to furniture and tools — with zero data ever leaving your device.

KEY FEATURES

• Offline-First & Air-Gapped
  The app declares NO internet permission. Your data physically cannot be
  transmitted anywhere. Everything lives exclusively on your device.

• Encrypted Database
  All records are stored in an AES-256 encrypted SQLite database (SQLCipher),
  protected by a key stored in the Android Keystore hardware-backed store.

• Warranty Tracking
  Log manufacturer and extended warranties with expiry dates. Receive daily
  local notifications before warranties lapse — no cloud required.

• Maintenance Logs
  Record service history for appliances, HVAC, vehicles, and more. Schedule
  recurring reminders via WorkManager to keep maintenance on track.

• Photo Documentation
  Capture up to 3 photos per item with the built-in camera. Images stay in
  your app's private folder, never uploaded or shared.

• Smart Categories & Search
  Organise items by category (Electronics, Appliances, Furniture, Tools…)
  and location (Living Room, Kitchen, Garage…). Full-text search and filter
  chips make finding items instant.

• Backup & Restore
  Export a password-less ZIP archive to any location you choose using Android's
  Storage Access Framework. Import on any device — no account needed.

• Material You Design
  Adapts to your device's dynamic colour theme for a native Android 12+
  experience.

PRIVACY COMMITMENT

• No accounts. No sign-in. No cloud sync.
• No analytics, crash reporting, or advertising SDKs.
• No internet permission — verified via Android manifest.
• All images and records remain exclusively on your local device.

Home Asset Keeper is built for people who take privacy seriously and want a
reliable, beautifully designed inventory tool that works forever — even without
an internet connection.
```

---

### Privacy Policy

**Effective Date:** 2026-03-05
**App:** Home Asset Keeper
**Developer:** HomeAssetKeeper

#### 1. Introduction
This Privacy Policy describes how Home Asset Keeper ("the App", "we", "us")
handles information when you use the application. The App is designed with a
privacy-first, offline-only architecture. We do not operate any servers, and we
do not collect, transmit, store, or process any personal data outside of your
device.

#### 2. No Internet Access
The App does **not** declare the `android.permission.INTERNET` permission in its
Android manifest. This means the App is technically incapable of initiating any
network connection. No data can be sent to or received from any remote server,
including our own.

#### 3. Data Collected
We collect **no data whatsoever**. The App does not collect:

- Personal identifiers (name, email, phone number, address)
- Device identifiers (IMEI, advertising ID, Android ID)
- Location data
- Photos or images (images you capture stay exclusively in the App's private
  storage folder on your device and are never read by any third party)
- Usage analytics or crash logs
- Any other information

#### 4. Data Storage
All information you enter into the App — item records, warranty dates,
maintenance logs, photos — is stored exclusively in:

- An AES-256 encrypted Room/SQLite database on your device's internal storage
- Your device's private external files directory (`/Android/data/<package>/files/`)

The encryption key is generated and stored in the Android Keystore, a
hardware-backed secure store. Neither we nor any third party can access this key.

#### 5. Third-Party SDKs
The App contains **no third-party analytics, advertising, crash-reporting, or
telemetry SDKs**. All open-source libraries used (Jetpack, Room, SQLCipher,
CameraX, Coil, WorkManager, Hilt) operate entirely on-device and do not
transmit data.

#### 6. Backup & Export
The App provides an optional backup feature that writes a ZIP archive to a
location **you select** on your own device using the Android Storage Access
Framework. This file is under your sole control. We have no access to it.

#### 7. Permissions Used
| Permission | Purpose |
|---|---|
| `CAMERA` | Capture item photos on-device |
| `POST_NOTIFICATIONS` | Show local warranty/maintenance reminders |

No network, location, contacts, or storage permissions are requested.

#### 8. Children's Privacy
The App does not knowingly collect any information from anyone, including
children under the age of 13, because it does not collect any information at all.

#### 9. Changes to This Policy
If we make material changes to this policy (e.g., adding a new permission), we
will update the effective date and provide an updated description in the Play
Store listing prior to releasing the updated version.

#### 10. Contact
If you have questions about this Privacy Policy, please open an issue at the
project's source repository or contact the developer through the Play Store
listing page.

---

---

## 繁體中文 (Traditional Chinese)

### 應用程式名稱 *(最多 30 字元)*

```
家庭資產管家
```
*(6 個字元)*

---

### 簡短說明 *(最多 80 字元)*

```
離線追蹤家庭資產、保固與維護紀錄——完全不需要網路，零雲端。
```
*(30 個字元)*

---

### 完整說明 *(最多 4000 字元)*

```
家庭資產管家是您的私人、離線優先的家庭物品清單管理工具。
為家中每一件物品——家電、電子產品、家具、工具——拍照、建檔、追蹤，
所有資料完全保留在您的裝置上，絕不對外傳輸。

主要功能

• 完全離線、氣隙隔離
  本應用程式不宣告任何網路權限（INTERNET permission），
  資料在技術上根本無法被傳送至任何地方，一切皆存放於您的裝置之中。

• 加密資料庫
  所有記錄儲存於 AES-256 加密的 SQLite 資料庫（SQLCipher），
  加密金鑰由 Android Keystore 硬體安全模組保護。

• 保固追蹤
  登錄原廠保固與延伸保固的到期日。保固即將到期前，
  系統會發送本地通知提醒您——不需雲端服務。

• 維護記錄
  記錄家電、暖通空調、車輛等的維修歷史，並透過 WorkManager
  設定定期提醒，讓維護工作不遺漏。

• 相片紀錄
  每件物品最多可拍攝 3 張照片，相片存放於應用程式的私有資料夾，
  從不上傳或分享。

• 智慧分類與搜尋
  依類別（電子產品、家電、家具、工具……）與位置
  （客廳、廚房、車庫……）整理物品。全文搜尋與篩選標籤讓尋找物品更快速。

• 備份與還原
  透過 Android 儲存存取框架，將 ZIP 備份匯出至您選擇的位置；
  在任何裝置上匯入——不需帳號。

• Material You 設計
  支援 Android 12 及以上版本的動態顏色主題，提供原生 Android 體驗。

隱私承諾

• 無帳號。無需登入。無雲端同步。
• 不包含任何分析、崩潰回報或廣告 SDK。
• 無網路權限——透過 Android Manifest 可驗證。
• 所有相片與記錄完全保留在您的本地裝置上。

家庭資產管家是為重視隱私、需要可靠且精心設計的清單工具的使用者所打造，
即使沒有網路連線，也能永久正常運作。
```

---

### 隱私權政策

**生效日期：** 2026 年 3 月 5 日
**應用程式：** 家庭資產管家（Home Asset Keeper）
**開發者：** HomeAssetKeeper

#### 1. 前言
本隱私權政策說明「家庭資產管家」（以下簡稱「本應用程式」、「我們」）
在您使用本應用程式時如何處理資訊。本應用程式採用隱私優先、僅限離線的
架構。我們不運營任何伺服器，亦不在您的裝置以外收集、傳輸、儲存或處理
任何個人資料。

#### 2. 無網路存取
本應用程式的 Android Manifest **不宣告** `android.permission.INTERNET`
權限。這意味著本應用程式在技術上無法發起任何網路連線，任何資料均無法
傳送至任何遠端伺服器（包括我方伺服器）。

#### 3. 收集的資料
我們**完全不收集任何資料**。本應用程式不收集：

- 個人識別資訊（姓名、電子郵件、電話號碼、地址）
- 裝置識別碼（IMEI、廣告 ID、Android ID）
- 位置資料
- 相片或影像（您拍攝的相片僅存放於本應用程式在您裝置上的私有儲存資料夾，
  任何第三方均無法讀取）
- 使用分析或崩潰日誌
- 任何其他資訊

#### 4. 資料儲存
您在應用程式中輸入的所有資訊——物品記錄、保固日期、維護記錄、相片——
專屬存放於：

- 裝置內部儲存空間中經 AES-256 加密的 Room/SQLite 資料庫
- 裝置的私有外部檔案目錄（`/Android/data/<package>/files/`）

加密金鑰由 Android Keystore（硬體支援的安全儲存）生成並保護。
我們及任何第三方均無法存取此金鑰。

#### 5. 第三方 SDK
本應用程式**不包含任何第三方**分析、廣告、崩潰回報或遙測 SDK。
所使用的開源程式庫（Jetpack、Room、SQLCipher、CameraX、Coil、
WorkManager、Hilt）均完全在裝置上運作，不傳輸任何資料。

#### 6. 備份與匯出
本應用程式提供選用的備份功能，透過 Android 儲存存取框架將 ZIP 壓縮檔
寫入**您選擇**的位置。該檔案完全由您掌控，我們無法存取。

#### 7. 使用的權限
| 權限 | 用途 |
|---|---|
| `CAMERA` | 在裝置上拍攝物品相片 |
| `POST_NOTIFICATIONS` | 顯示本地保固/維護提醒通知 |

不要求網路、位置、聯絡人或儲存空間權限。

#### 8. 兒童隱私
本應用程式不會刻意向任何人（包括 13 歲以下兒童）收集任何資訊，
因為本應用程式根本不收集任何資訊。

#### 9. 政策變更
若我們對本政策進行重大變更（例如新增權限），我們將更新生效日期，
並在發布更新版本前於 Play 商店說明中提供更新描述。

#### 10. 聯絡方式
若您對本隱私權政策有任何疑問，請透過 Play 商店列表頁面聯絡開發者，
或在專案原始碼儲存庫提交 Issue。
