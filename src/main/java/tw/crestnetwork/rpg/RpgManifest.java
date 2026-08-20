package tw.crestnetwork.rpg;

import java.util.List;

record RpgManifest(
        String checksum,
        List<RpgMonsterDefinition> monsters,
        List<RpgWeaponDefinition> weapons,
        List<RpgEquipmentDefinition> equipments,
        List<RpgItemDefinition> items,
        List<RpgGameplayDefinition> gameplay,
        long lastRevisionId
) {}
