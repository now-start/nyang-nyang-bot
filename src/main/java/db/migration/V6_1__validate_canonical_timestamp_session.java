package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V6_1__validate_canonical_timestamp_session extends BaseJavaMigration {

    @Override
    public Integer getChecksum() {
        return CanonicalMigrationSupport.checksum(getClass(), false);
    }

    @Override
    public void migrate(Context context) throws Exception {
        if (CanonicalMigrationSupport.isMariaDb(context)) {
            CanonicalMigrationSupport.requireAsiaSeoulSession(context.getConnection());
        }
    }
}
