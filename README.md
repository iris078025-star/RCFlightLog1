# RC Flight Log — Android 原生專案

## 功能
- 飛機新增/編輯/刪除；相簿照片；左右滑動切換飛機
- 每台飛機獨立電池；電池編號、容量、初始使用次數
- 每次飛行自動記錄日期時間及分鐘秒數；今日使用狀態；歷史刪除
- 離線 SQLite 資料庫；JSON 匯出/匯入

## 建置 APK
1. 在電腦安裝 Android Studio（含 Android SDK 35）。
2. 開啟此資料夾，等待 Gradle 同步（需要網路下載相依套件）。
3. 選 Build > Build Bundle(s) / APK(s) > Build APK(s)。
4. 產生的 APK 通常在 `app/build/outputs/apk/debug/app-debug.apk`。
5. 將 APK 傳到 Galaxy Tab S8 安裝（需允許該來源安裝應用程式）。

## 注意
- 本交付為原生 Android Studio 原始碼專案，尚未在 Android SDK 上實際編譯測試。
- 每筆飛行加一是「使用次數」，不是等效完整循環 EFC。
- JSON 備份目前僅保存資料庫內的照片路徑，**不包含照片檔案**；換機後照片可能無法顯示。
- 使用 ACTION_GET_CONTENT 選照片後會複製到 App 私有儲存空間。
- 建議先在測試資料上驗證匯入覆蓋及刪除功能。
