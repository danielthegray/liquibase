package liquibase.diff.output.changelog.core;

import liquibase.change.AddColumnConfig;
import liquibase.change.Change;
import liquibase.change.ConstraintsConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.datatype.DatabaseDataType;
import liquibase.datatype.LiquibaseDataType;
import liquibase.diff.output.DiffOutputControl;
import liquibase.diff.output.changelog.AbstractChangeGenerator;
import liquibase.diff.output.changelog.ChangeGeneratorChain;
import liquibase.diff.output.changelog.MissingObjectChangeGenerator;
import liquibase.structure.DatabaseObject;
import liquibase.structure.core.Column;
import liquibase.structure.core.PrimaryKey;
import liquibase.structure.core.Table;
import liquibase.structure.core.View;

public class MissingColumnChangeGenerator extends AbstractChangeGenerator implements MissingObjectChangeGenerator {

    @Override
    public int getPriority(Class<? extends DatabaseObject> objectType, Database database) {
        if (Column.class.isAssignableFrom(objectType)) {
            return PRIORITY_DEFAULT;
        }
        return PRIORITY_NONE;

    }

    @Override
    public Class<? extends DatabaseObject>[] runAfterTypes() {
        return new Class[] {
                Table.class
        };
    }

    @Override
    public Class<? extends DatabaseObject>[] runBeforeTypes() {
        return new Class[] { PrimaryKey.class };
    }

    @Override
    public Change[] fixMissing(DatabaseObject missingObject, DiffOutputControl control, Database referenceDatabase, Database comparisonDatabase, ChangeGeneratorChain chain) {
        Column column = (Column) missingObject;

        if (column.getRelation() instanceof View) {
            return null;
        }

        if (column.getRelation().getSnapshotId() == null) { //not an actual table, maybe an alias, maybe in a different schema. Don't fix it.
            return null;
        }


        AddColumnChange change = createAddColumnChange();
        change.setTableName(column.getRelation().getName());
        if (control.getIncludeCatalog()) {
            change.setCatalogName(column.getRelation().getSchema().getCatalogName());
        }
        if (control.getIncludeSchema()) {
            change.setSchemaName(column.getRelation().getSchema().getName());
        }

        AddColumnConfig columnConfig = createAddColumnConfig();
        columnConfig.setName(column.getName());

        LiquibaseDataType ldt = DataTypeFactory.getInstance().from(column.getType(), referenceDatabase);
        DatabaseDataType ddt = ldt.toDatabaseDataType(comparisonDatabase);
        String typeString = ddt.toString();
        columnConfig.setType(typeString);

        MissingTableChangeGenerator.setDefaultValue(columnConfig, column, comparisonDatabase);

        Column.AutoIncrementInformation autoIncrementInfo = column.getAutoIncrementInformation();
        if (autoIncrementInfo != null) {
            columnConfig.setAutoIncrement(true);
            columnConfig.setGenerationType(autoIncrementInfo.getGenerationType());
            columnConfig.setDefaultOnNull(autoIncrementInfo.getDefaultOnNull());
        }

        if (column.getRemarks() != null) {
            columnConfig.setRemarks(column.getRemarks());
        }
        ConstraintsConfig constraintsConfig = columnConfig.getConstraints();
        if ((column.isNullable() != null) && !column.isNullable()) {
            if (constraintsConfig == null) {
                constraintsConfig = new ConstraintsConfig();
            }
            constraintsConfig.setNullable(false);
            constraintsConfig.setNotNullConstraintName(column.getAttribute("notNullConstraintName", String.class));
            if (!column.getValidateNullable()) {
                constraintsConfig.setValidateNullable(false);
            }
        }
        if (constraintsConfig != null) {
            columnConfig.setConstraints(constraintsConfig);
        }

        change.addColumn(columnConfig);

        return new Change[] { change };
    }

    protected AddColumnConfig createAddColumnConfig() {
        return new AddColumnConfig();
    }

    protected AddColumnChange createAddColumnChange() {
        return new AddColumnChange();
    }
}
