package liquibase.change.core;

import liquibase.change.*;
import liquibase.database.Database;
import liquibase.database.core.InformixDatabase;
import liquibase.exception.ValidationErrors;
import liquibase.snapshot.SnapshotGeneratorFactory;
import liquibase.statement.InsertExecutablePreparedStatement;
import liquibase.statement.SqlStatement;
import liquibase.statement.core.InsertStatement;
import liquibase.structure.core.Column;

import java.util.ArrayList;
import java.util.List;

/**
 * Inserts data into an existing table.
 */
@DatabaseChange(name="insert", description = "Inserts data into an existing table", priority = ChangeMetaData.PRIORITY_DEFAULT, appliesTo = "table")
public class InsertDataChange extends AbstractChange implements ChangeWithColumns<ColumnConfig>, DbmsTargetedChange {

    private String catalogName;
    private String schemaName;
    private String tableName;
    private List<ColumnConfig> columns;
    private String dbms;

    public InsertDataChange() {
        columns = new ArrayList<>();
    }

    @Override
    public ValidationErrors validate(Database database) {
        ValidationErrors validate = super.validate(database);
        validate.checkRequiredField("columns", columns);
        return validate;
    }

    @DatabaseChangeProperty(mustEqualExisting ="table.catalog", since = "3.0")
    public String getCatalogName() {
        return catalogName;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    @DatabaseChangeProperty(mustEqualExisting ="table.schema")
    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    @DatabaseChangeProperty(mustEqualExisting = "table", description = "Name of the table to insert data into")
    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    @Override
    @DatabaseChangeProperty(mustEqualExisting = "table.column", description = "Data to insert into columns", requiredForDatabase = "all")
    public List<ColumnConfig> getColumns() {
        return columns;
    }

    @Override
    public void setColumns(List<ColumnConfig> columns) {
        this.columns = columns;
    }

    @Override
    public void addColumn(ColumnConfig column) {
        columns.add(column);
    }

    public void removeColumn(ColumnConfig column) {
        columns.remove(column);
    }

    @Override
    public SqlStatement[] generateStatements(Database database) {

        boolean needsPreparedStatement = false;
        for (ColumnConfig column : columns) {
            if (column.getValueBlobFile() != null) {
                needsPreparedStatement = true;
            }
            if (column.getValueClobFile() != null) {
                needsPreparedStatement = true;
            }

            if (!needsPreparedStatement && database instanceof InformixDatabase) {
                if (column.getValue() != null) {
                    try {
                        Column snapshot = SnapshotGeneratorFactory.getInstance().createSnapshot(new Column(column), database);
                        if (snapshot != null) {
                            needsPreparedStatement = true;
                        }
                    } catch (Exception ignore) { //assume it's not a clob
                    }
                }
            }
        }

        if (needsPreparedStatement) {
            return new SqlStatement[] {
                    new InsertExecutablePreparedStatement(database, catalogName, schemaName, tableName, columns, getChangeSet(), this.getResourceAccessor())
            };
        }


        InsertStatement statement = new InsertStatement(getCatalogName(), getSchemaName(), getTableName());

        for (ColumnConfig column : columns) {

        	if (database.supportsAutoIncrement()
        			&& column.isAutoIncrement() != null && column.isAutoIncrement()) {
            	// skip auto increment columns as they will be generated by the database
            	continue;
            }

            statement.addColumnValue(column.getName(), column.getValueObject());
        }
        return new SqlStatement[]{
                statement
        };
    }

    @Override
    public ChangeStatus checkStatus(Database database) {
        return new ChangeStatus().unknown("Cannot check insertData status");
    }

    /**
     * @see liquibase.change.Change#getConfirmationMessage()
     */
    @Override
    public String getConfirmationMessage() {
        return "New row inserted into " + getTableName();
    }

    @Override
    @DatabaseChangeProperty(since = "3.0", exampleValue = "h2, oracle")
    public String getDbms() {
        return dbms;
    }

    @Override
    public void setDbms(final String dbms) {
        this.dbms = dbms;
    }

    @Override
    public String getSerializedObjectNamespace() {
        return STANDARD_CHANGELOG_NAMESPACE;
    }
}
