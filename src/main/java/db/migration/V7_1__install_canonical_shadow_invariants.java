package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

public final class V7_1__install_canonical_shadow_invariants extends BaseJavaMigration {

    @Override
    public Integer getChecksum() {
        return CanonicalMigrationSupport.checksum(getClass(), true);
    }

    @Override
    public void migrate(Context context) throws Exception {
        if (CanonicalMigrationSupport.isMariaDb(context)) {
            CanonicalMigrationSupport.dropInvariants(context.getConnection(), true);
            CanonicalMigrationSupport.installInvariants(context.getConnection(), true);
        }
    }
}
