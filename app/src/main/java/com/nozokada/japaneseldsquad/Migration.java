package com.nozokada.japaneseldsquad;

import io.realm.DynamicRealm;
import io.realm.RealmMigration;
import io.realm.RealmSchema;

public class Migration implements RealmMigration {

    @Override
    public void migrate(DynamicRealm realm, long oldVersion, long newVersion) {

        RealmSchema schema = realm.getSchema();

        if (!schema.get("Scripture").hasField("scripture_jpn_search")) {
            schema.get("Scripture")
                    .addField("scripture_jpn_search", String.class)
                    .addField("scripture_eng_search", String.class);

            schema.get("Scripture")
                    .setNullable("scripture_jpn_search", false)
                    .setNullable("scripture_eng_search", false);
        }
    }
}
