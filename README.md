# CrestRPG

Crest Network 的 Purpur 26.2／Java 25 獨立 RPG 引擎。它從網站同步已發布內容，也提供遊戲內安全 GUI 編輯器、裝備與技能系統。

## 主要功能

- 自訂怪物、武器、裝備、物品與掉落表
- 詞綴、寶石、插槽、稀有度、品質與隨機生成
- 套裝、鑑定、強化、鑲嵌與分解
- 自訂製作站、任務、隊伍、工會、地城、稱號與成就
- 職業、技能樹、技能快捷列、技能等級與法力
- 遊戲內 RPG 內容編輯器與網站 manifest 同步
- Oraxen、ModelEngine、Vault 與 PlaceholderAPI 整合
- 背包與箱子整理工具

本版本是 CrestRPG 獨立引擎，不包含 MMOItems／MMOCore 匯入程式。

## 建置

需求：JDK 25。

```bash
./gradlew clean build
```

產物：`build/libs/CrestRPG-2.8.0.jar`

## 安裝

1. 將 JAR 放入 Purpur 26.2 伺服器的 `plugins/`。
2. 啟動一次後編輯 `plugins/CrestRPG/config.yml`。
3. 如需網站同步，設定 API URL、token 與唯一 `server-key`。
4. 根據使用的功能安裝 Oraxen、ModelEngine、Vault 或 PlaceholderAPI。
5. 重新啟動伺服器。

儲存庫內只有安全範例設定；不要提交正式 API token 或資料庫密碼。

## 內建網頁編輯器

CrestRPG 可像 BlueMap 一樣由插件自己提供網頁，不需要另外部署網站。在 `plugins/CrestRPG/config.yml` 設定：

```yaml
web-editor:
  enabled: true
  bind-address: "127.0.0.1"
  port: 8765
  token: "CHANGE_ME"
```

重新啟動伺服器後，插件會自動產生安全 Token、寫回 `config.yml` 並在主控台顯示一次。使用該 Token 登入 `http://127.0.0.1:8765/`，之後可在後台安全設定中立即更換。網頁後台與 `/crestrpg editor` 共用同一份 `editor-drafts` 資料，支援全分類瀏覽、搜尋、新增、複製、JSON 編輯、刪除與全量驗證。

第一次啟用網頁編輯器時，插件會將現有 `items.yml` 的武器與裝備、`quests.yml` 的任務，以及四種預設稀有度匯入草稿清單。匯入只執行一次，不會在之後覆蓋管理員的修改。

草稿預設使用圖形化欄位、巢狀設定與可增減的清單；完整 JSON 僅保留於進階模式。物品材質可在遊戲內直接從原版圖示選擇，網頁後台也可由 Mojang 官方素材索引同步物品與方塊圖示，版本由 `minecraft-assets.version` 設定。「素材與資源包」可上傳最大 5 MB 的 PNG 貼圖與模型 JSON，自動分配 `CustomModelData`、產生原版資源包 ZIP、SHA-1 及 `generated/oraxen/items/crestrpg.yml`。`resource-pack.pack-format` 必須依目標 Minecraft 版本調整。

預設只允許伺服器本機連線。若要開放外網，請放在 HTTPS 反向代理後方，限制來源 IP，並使用強隨機 Token。

## 玩家指令

- `/skills`、`/quests`
- `/rpgitem`、`/rpgclass`、`/rpgskill`、`/skilltree`
- `/rpgcraft`、`/party`、`/titles`
- `/cls`

## 管理指令

- `/crestrpg reload|editor|list`
- `/crestrpg weapons|equipments|items`
- `/crestrpg spawn|give`

## 授權

本專案目前未附開放原始碼授權。公開可見不代表允許複製、修改或重新散布。
