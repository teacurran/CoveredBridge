package app.coveredbridge.data.jpa;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.jvnet.inflector.Noun;

public class ModelNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

  @Override
  public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {

    final Identifier tableIdentifier = classToTableName(name, jdbcEnvironment);

    if (tableIdentifier == null) {
      return super.toPhysicalTableName(name, jdbcEnvironment);
    }
    final String tableName = tableIdentifier.getText();

    if (tableName.contains("_")) {
      final String lastWord = tableName.substring(tableName.lastIndexOf("_") + 1);
      final String prefix = tableName.substring(0, tableName.lastIndexOf("_"));
      final String combined = String.format("%s_%s", prefix, Noun.pluralOf(lastWord));
      return this.getIdentifier(Noun.pluralOf(combined), name.isQuoted(), jdbcEnvironment);
    } else {
      return this.getIdentifier(Noun.pluralOf(tableName), name.isQuoted(), jdbcEnvironment);
    }
  }

  private Identifier classToTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
    if (name == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder(name.getText().replace('.', '_'));

    for (int i = 1; i < builder.length() - 1; ++i) {
      if (this.needsUnderscore(builder.charAt(i - 1), builder.charAt(i), builder.charAt(i + 1))) {
        builder.insert(i++, '_');
      }
    }

    return this.getIdentifier(builder.toString(), name.isQuoted(), jdbcEnvironment);
  }

  private boolean needsUnderscore(char before, char current, char after) {
    return Character.isLowerCase(before) && Character.isUpperCase(current) && Character.isLowerCase(after);
  }

}
