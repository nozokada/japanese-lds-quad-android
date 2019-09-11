package com.nozokada.japaneseldsquad;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

public class Migration implements RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {
        RealmSchema schema = realm.getSchema();
        if (oldVersion < 2) {
            if (schema.get("Scripture").hasField("scripture_jpn_search")) {
                schema.get("Scripture")
                        .renameField("scripture_jpn_search", "scripture_primary_raw")
                        .renameField("scripture_eng_search", "scripture_secondary_raw");
            }
            if (!schema.get("Book").hasField("name_primary")) {
                schema.get("Book")
                        .renameField("name_jpn", "name_primary")
                        .renameField("name_eng", "name_secondary");
            }
            if(!schema.get("Scripture").hasField("scripture_primary")) {
                schema.get("Scripture")
                        .renameField("scripture_jpn", "scripture_primary")
                        .renameField("scripture_eng", "scripture_secondary");
            }
            if(!schema.get("Bookmark").hasField("name_primary")) {
                schema.get("Bookmark")
                        .renameField("name_jpn", "name_primary")
                        .renameField("name_eng", "name_secondary");
            }
        }
    }
}
