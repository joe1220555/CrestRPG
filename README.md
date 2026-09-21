# CrestRPG

Crest Network 的 Purpur 26.2／Java 25 獨立 RPG 引擎。它從網站同步已發布內容，也提供遊戲內安全 GUI 編輯器、裝備與技能系統。

## 主要功能

- 自訂怪物、武器、裝備、物品與掉落表
- 詞綴、寶石、插槽、稀有度、品質與隨機生成
- 套裝、鑑定、強化、鑲嵌與分解
- 自訂製作站、任務、隊伍、地城、稱號與成就
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

產物：`build/libs/CrestRPG-2.1.0.jar`

## 安裝

1. 將 JAR 放入 Purpur 26.2 伺服器的 `plugins/`。
2. 啟動一次後編輯 `plugins/CrestRPG/config.yml`。
3. 如需網站同步，設定 API URL、token 與唯一 `server-key`。
4. 根據使用的功能安裝 Oraxen、ModelEngine、Vault 或 PlaceholderAPI。
5. 重新啟動伺服器。

儲存庫內只有安全範例設定；不要提交正式 API token 或資料庫密碼。

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
