# 不用電腦，在 Galaxy Tab S8 編譯 APK

1. 在平板瀏覽器開啟 https://github.com 並登入或註冊。
2. 新建 **Private** repository，名稱例如 `RCFlightLog`。
3. 將此 ZIP 解壓縮；把 **RCFlightLog 資料夾內**的 `app/`、`build.gradle.kts`、`settings.gradle.kts`、`.github/` 等內容放在 repository **根目錄**，不要多包一層 RCFlightLog。注意 `.github` 是隱藏資料夾，檔案管理器需開啟「顯示隱藏檔案」。
4. 在 GitHub repository 的 **Actions** 頁面，選 **Build Android APK**，點 **Run workflow**。如果第一次上傳已觸發 push，直接查看該次執行即可。
5. 等待執行完成（綠色勾）。點進該次 workflow，在 **Artifacts** 下載 `RCFlightLog-debug-APK` ZIP。
6. 解壓縮得到 `app-debug.apk`，在 Tab S8 點開安裝，並依 Android 提示允許瀏覽器或檔案管理器「安裝未知應用程式」。

**注意：** 這是測試用 debug APK，尚未在此環境完成實際編譯與實機驗證。若 workflow 失敗，請提供 Actions 的紅色錯誤訊息以便修正。未來若需正式發佈、升級安裝不遺失資料，請建立並妥善保管正式簽署金鑰；不要公開上傳簽署私鑰。
