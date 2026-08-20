package com.example.crestrpg.database;

import tw.crestnetwork.rpg.CrestRpgPlugin;
import com.example.crestrpg.skills.PlayerProfile;
import com.example.crestrpg.skills.SkillType;
import com.example.crestrpg.quests.QuestProgress;

import java.io.File;
import java.sql.*;
import java.util.*;

public class RPGDatabase {

    private final CrestRpgPlugin plugin;
    private final File dbFile;
    private Connection connection;
    private boolean useMySQL;

    public RPGDatabase(CrestRpgPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.dbFile = new File(dataFolder, "rpg.db");
        this.useMySQL = plugin.getConfig().getBoolean("database.use-mysql", false);
        initialize();
    }

    private synchronized void initialize() {
        try {
            if (useMySQL) {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } else {
                Class.forName("org.sqlite.JDBC");
            }

            try (Statement statement = getConnection().createStatement()) {
                // Table 1: Skills and AP
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS crest_skills (" +
                                "uuid VARCHAR(36) PRIMARY KEY," +
                                "player_name VARCHAR(16) NOT NULL," +
                                "level_farming INT DEFAULT 1, xp_farming DOUBLE DEFAULT 0," +
                                "level_mining INT DEFAULT 1, xp_mining DOUBLE DEFAULT 0," +
                                "level_foraging INT DEFAULT 1, xp_foraging DOUBLE DEFAULT 0," +
                                "level_combat INT DEFAULT 1, xp_combat DOUBLE DEFAULT 0," +
                                "level_archery INT DEFAULT 1, xp_archery DOUBLE DEFAULT 0," +
                                "level_sorcery INT DEFAULT 1, xp_sorcery DOUBLE DEFAULT 0," +
                                "level_defense INT DEFAULT 1, xp_defense DOUBLE DEFAULT 0," +
                                "stat_strength INT DEFAULT 0," +
                                "stat_dexterity INT DEFAULT 0," +
                                "stat_intelligence INT DEFAULT 0," +
                                "stat_vitality INT DEFAULT 0," +
                                "unused_ap INT DEFAULT 0," +
                                "character_level INT DEFAULT 1," +
                                "class_id VARCHAR(64) DEFAULT 'adventurer'," +
                                "skill_points INT DEFAULT 1," +
                                "character_xp DOUBLE DEFAULT 0" +
                                ");"
                );

                // Run migration to add character_level column to existing table
                try {
                    statement.executeUpdate("ALTER TABLE crest_skills ADD COLUMN character_level INT DEFAULT 1;");
                } catch (SQLException ignored) {}
                try {
                    statement.executeUpdate("ALTER TABLE crest_skills ADD COLUMN class_id VARCHAR(64) DEFAULT 'adventurer';");
                } catch (SQLException ignored) {}
                try {
                    statement.executeUpdate("ALTER TABLE crest_skills ADD COLUMN skill_points INT DEFAULT 1;");
                } catch (SQLException ignored) {}
                try {
                    statement.executeUpdate("ALTER TABLE crest_skills ADD COLUMN character_xp DOUBLE DEFAULT 0;");
                } catch (SQLException ignored) {}
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS crest_unlocked_skill_nodes (" +
                                "uuid VARCHAR(36) NOT NULL," +
                                "node_key VARCHAR(129) NOT NULL," +
                                "PRIMARY KEY (uuid, node_key)" +
                                ");"
                );
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS crest_class_skill_progress (uuid VARCHAR(36) NOT NULL, skill_key VARCHAR(64) NOT NULL, skill_level INT DEFAULT 1, skill_xp DOUBLE DEFAULT 0, PRIMARY KEY (uuid, skill_key));");
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS crest_skill_bar (uuid VARCHAR(36) NOT NULL, bar_slot INT NOT NULL, skill_key VARCHAR(64) NOT NULL, PRIMARY KEY (uuid, bar_slot));");

                // Table 2: Active Quests
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS active_quests (" +
                                "uuid VARCHAR(36) NOT NULL," +
                                "quest_id VARCHAR(64) NOT NULL," +
                                "objective_index INT NOT NULL," +
                                "current_progress INT DEFAULT 0," +
                                "PRIMARY KEY (uuid, quest_id, objective_index)" +
                                ");"
                );

                // Table 3: Completed Quests
                statement.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS completed_quests (" +
                                "uuid VARCHAR(36) NOT NULL," +
                                "quest_id VARCHAR(64) NOT NULL," +
                                "completion_time BIGINT NOT NULL," +
                                "PRIMARY KEY (uuid, quest_id)" +
                                ");"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            if (useMySQL) {
                String host = plugin.getConfig().getString("database.host", "localhost");
                int port = plugin.getConfig().getInt("database.port", 3306);
                String dbName = plugin.getConfig().getString("database.database", "huskysync");
                String username = plugin.getConfig().getString("database.username", "root");
                String password = plugin.getConfig().getString("database.password", "");
                boolean ssl = plugin.getConfig().getBoolean("database.ssl", false);

                String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName +
                             "?useSSL=" + ssl + "&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
                connection = DriverManager.getConnection(url, username, password);
            } else {
                connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            }
        }
        return connection;
    }

    public synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ==========================================
    // Skills Data Methods
    // ==========================================

    public synchronized PlayerProfile loadProfile(UUID uuid, String name) {
        String sql = "SELECT * FROM crest_skills WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    PlayerProfile profile = new PlayerProfile(uuid, name);
                    profile.setPlayerName(rs.getString("player_name"));
                    profile.setStrength(rs.getInt("stat_strength"));
                    profile.setDexterity(rs.getInt("stat_dexterity"));
                    profile.setIntelligence(rs.getInt("stat_intelligence"));
                    profile.setVitality(rs.getInt("stat_vitality"));
                    profile.setUnusedAp(rs.getInt("unused_ap"));
                    profile.setCharacterLevel(rs.getInt("character_level"));
                    profile.setClassId(rs.getString("class_id"));
                    profile.setSkillPoints(rs.getInt("skill_points"));
                    profile.setCharacterXp(rs.getDouble("character_xp"));
                    loadUnlockedSkillNodes(profile);
                    loadClassSkills(profile);

                    for (SkillType type : SkillType.values()) {
                        String key = type.getKey().toLowerCase();
                        profile.setLevel(type, rs.getInt("level_" + key));
                        profile.setXp(type, rs.getDouble("xp_" + key));
                    }
                    return profile;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Return a fresh new profile and insert it
        PlayerProfile newProfile = new PlayerProfile(uuid, name);
        createProfile(newProfile);
        return newProfile;
    }

    private synchronized void createProfile(PlayerProfile profile) {
        String sql = "INSERT INTO crest_skills(uuid, player_name, level_farming, xp_farming, level_mining, xp_mining, " +
                "level_foraging, xp_foraging, level_combat, xp_combat, level_archery, xp_archery, level_sorcery, xp_sorcery, " +
                "level_defense, xp_defense, stat_strength, stat_dexterity, stat_intelligence, stat_vitality, unused_ap, character_level, class_id, skill_points, character_xp) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, profile.getUuid().toString());
            pstmt.setString(2, profile.getPlayerName());

            int idx = 3;
            for (SkillType type : SkillType.values()) {
                pstmt.setInt(idx++, profile.getLevel(type));
                pstmt.setDouble(idx++, profile.getXp(type));
            }

            pstmt.setInt(idx++, profile.getStrength());
            pstmt.setInt(idx++, profile.getDexterity());
            pstmt.setInt(idx++, profile.getIntelligence());
            pstmt.setInt(idx++, profile.getVitality());
            pstmt.setInt(idx++, profile.getUnusedAp());
            pstmt.setInt(idx++, profile.getCharacterLevel());
            pstmt.setString(idx, profile.getClassId());
            pstmt.setInt(++idx, profile.getSkillPoints());
            pstmt.setDouble(++idx, profile.getCharacterXp());

            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void saveProfile(PlayerProfile profile) {
        String sql = "UPDATE crest_skills SET player_name = ?, " +
                "level_farming = ?, xp_farming = ?, level_mining = ?, xp_mining = ?, " +
                "level_foraging = ?, xp_foraging = ?, level_combat = ?, xp_combat = ?, " +
                "level_archery = ?, xp_archery = ?, level_sorcery = ?, xp_sorcery = ?, " +
                "level_defense = ?, xp_defense = ?, " +
                "stat_strength = ?, stat_dexterity = ?, stat_intelligence = ?, stat_vitality = ?, " +
                "unused_ap = ?, character_level = ?, class_id = ?, skill_points = ?, character_xp = ? WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, profile.getPlayerName());

            int idx = 2;
            for (SkillType type : SkillType.values()) {
                pstmt.setInt(idx++, profile.getLevel(type));
                pstmt.setDouble(idx++, profile.getXp(type));
            }

            pstmt.setInt(idx++, profile.getStrength());
            pstmt.setInt(idx++, profile.getDexterity());
            pstmt.setInt(idx++, profile.getIntelligence());
            pstmt.setInt(idx++, profile.getVitality());
            pstmt.setInt(idx++, profile.getUnusedAp());
            pstmt.setInt(idx++, profile.getCharacterLevel());
            pstmt.setString(idx++, profile.getClassId());
            pstmt.setInt(idx++, profile.getSkillPoints());
            pstmt.setDouble(idx++, profile.getCharacterXp());
            pstmt.setString(idx, profile.getUuid().toString());

            pstmt.executeUpdate();
            saveUnlockedSkillNodes(profile);
            saveClassSkills(profile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadClassSkills(PlayerProfile profile) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT skill_key, skill_level, skill_xp FROM crest_class_skill_progress WHERE uuid = ?")) {
            statement.setString(1, profile.getUuid().toString()); try (ResultSet result = statement.executeQuery()) {
                while (result.next()) { profile.getClassSkillLevels().put(result.getString(1), result.getInt(2)); profile.getClassSkillXp().put(result.getString(1), result.getDouble(3)); }
            }
        }
        try (PreparedStatement statement = getConnection().prepareStatement("SELECT bar_slot, skill_key FROM crest_skill_bar WHERE uuid = ?")) {
            statement.setString(1, profile.getUuid().toString()); try (ResultSet result = statement.executeQuery()) {
                while (result.next()) profile.getSkillBar().put(result.getInt(1), result.getString(2));
            }
        }
    }

    private void saveClassSkills(PlayerProfile profile) throws SQLException {
        try (PreparedStatement delete = getConnection().prepareStatement("DELETE FROM crest_class_skill_progress WHERE uuid = ?")) { delete.setString(1, profile.getUuid().toString()); delete.executeUpdate(); }
        try (PreparedStatement insert = getConnection().prepareStatement("INSERT INTO crest_class_skill_progress(uuid, skill_key, skill_level, skill_xp) VALUES(?,?,?,?)")) {
            for (Map.Entry<String, Integer> entry : profile.getClassSkillLevels().entrySet()) { insert.setString(1, profile.getUuid().toString()); insert.setString(2, entry.getKey()); insert.setInt(3, entry.getValue()); insert.setDouble(4, profile.getClassSkillXp(entry.getKey())); insert.addBatch(); } insert.executeBatch();
        }
        try (PreparedStatement delete = getConnection().prepareStatement("DELETE FROM crest_skill_bar WHERE uuid = ?")) { delete.setString(1, profile.getUuid().toString()); delete.executeUpdate(); }
        try (PreparedStatement insert = getConnection().prepareStatement("INSERT INTO crest_skill_bar(uuid, bar_slot, skill_key) VALUES(?,?,?)")) {
            for (Map.Entry<Integer, String> entry : profile.getSkillBar().entrySet()) { insert.setString(1, profile.getUuid().toString()); insert.setInt(2, entry.getKey()); insert.setString(3, entry.getValue()); insert.addBatch(); } insert.executeBatch();
        }
    }

    private void loadUnlockedSkillNodes(PlayerProfile profile) throws SQLException {
        try (PreparedStatement statement = getConnection().prepareStatement(
                "SELECT node_key FROM crest_unlocked_skill_nodes WHERE uuid = ?")) {
            statement.setString(1, profile.getUuid().toString());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) profile.getUnlockedSkillNodes().add(result.getString("node_key"));
            }
        }
    }

    private void saveUnlockedSkillNodes(PlayerProfile profile) throws SQLException {
        try (PreparedStatement delete = getConnection().prepareStatement(
                "DELETE FROM crest_unlocked_skill_nodes WHERE uuid = ?")) {
            delete.setString(1, profile.getUuid().toString());
            delete.executeUpdate();
        }
        try (PreparedStatement insert = getConnection().prepareStatement(
                "INSERT INTO crest_unlocked_skill_nodes(uuid, node_key) VALUES(?, ?)")) {
            for (String node : profile.getUnlockedSkillNodes()) {
                insert.setString(1, profile.getUuid().toString());
                insert.setString(2, node);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    // ==========================================
    // Quests Data Methods
    // ==========================================

    public synchronized Set<String> loadCompletedQuests(UUID uuid) {
        Set<String> list = new HashSet<>();
        String sql = "SELECT quest_id FROM completed_quests WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(rs.getString("quest_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    public synchronized Map<String, QuestProgress> loadActiveQuests(UUID uuid) {
        Map<String, QuestProgress> map = new HashMap<>();
        String sql = "SELECT * FROM active_quests WHERE uuid = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String questId = rs.getString("quest_id");
                    int objIdx = rs.getInt("objective_index");
                    int progVal = rs.getInt("current_progress");

                    QuestProgress progress = map.computeIfAbsent(questId, k -> new QuestProgress(uuid, questId));
                    progress.setProgress(objIdx, progVal);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public synchronized void saveQuestProgress(QuestProgress progress) {
        String sql = "REPLACE INTO active_quests(uuid, quest_id, objective_index, current_progress) VALUES(?,?,?,?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            Connection conn = getConnection();
            conn.setAutoCommit(false);
            for (Map.Entry<Integer, Integer> entry : progress.getProgressMap().entrySet()) {
                pstmt.setString(1, progress.getUuid().toString());
                pstmt.setString(2, progress.getQuestId());
                pstmt.setInt(3, entry.getKey());
                pstmt.setInt(4, entry.getValue());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            conn.commit();
            conn.setAutoCommit(true);
        } catch (Exception e) {
            e.printStackTrace();
            try { getConnection().rollback(); } catch (SQLException ex) { ex.printStackTrace(); }
        }
    }

    public synchronized void deleteQuestProgress(UUID uuid, String questId) {
        String sql = "DELETE FROM active_quests WHERE uuid = ? AND quest_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, questId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void addCompletedQuest(UUID uuid, String questId) {
        String sql = useMySQL ?
                "INSERT IGNORE INTO completed_quests(uuid, quest_id, completion_time) VALUES(?,?,?)" :
                "INSERT OR IGNORE INTO completed_quests(uuid, quest_id, completion_time) VALUES(?,?,?)";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, questId);
            pstmt.setLong(3, System.currentTimeMillis());
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized void removeCompletedQuest(UUID uuid, String questId) {
        String sql = "DELETE FROM completed_quests WHERE uuid = ? AND quest_id = ?";
        try (PreparedStatement pstmt = getConnection().prepareStatement(sql)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, questId);
            pstmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
