package org.activitymgr.core.orm;

import java.util.HashMap;
import java.util.Properties;
import java.util.TimeZone;

import org.activitymgr.core.orm.impl.DbClassMapper;

/**
 * Mapping de classe.
 * @author jbrazeau
 */
public class DbClassMapping {

	/** Logger */
	//private static Logger log = Logger.getLogger(DbClassMapping.class);

	/** Liste des mappers */
	private HashMap<Class<?>, DbClassMapper<?>> mappers = new HashMap<Class<?>, DbClassMapper<?>>();
	
	/** dictionnaire de propri�t�s de configuration */
	private Properties props;
	
	/** Timezone utilis� pour les dates */
	private TimeZone timeZone = TimeZone.getDefault();
	
	/**
	 * Constructeur par d�faut.
	 * @param props dictionnaire de propri�t�s de configuration.
	 */
	public DbClassMapping(Properties props) {
		this(props, TimeZone.getDefault());
	}

	/**
	 * Constructeur par d�faut.
	 * @param props dictionnaire de propri�t�s de configuration.
	 */
	public DbClassMapping(Properties props, TimeZone timeZone) {
		this.props = props;
		this.timeZone = timeZone;
	}
	
	/**
	 * Retourne l'instance singleton de mappeur de la classe.
	 * @param theClass la classe mapp�e.
	 * @return l'instance singleton de mappeur de la classe.
	 * @throws DbClassMappingException lev�e en cas de mauvaise configuration du
	 * 		mapping.
	 */
	@SuppressWarnings("unchecked")
	public <TYPE> IDbClassMapper<TYPE> getMapper(Class<TYPE> theClass) throws DbClassMappingException {
		DbClassMapper<TYPE> mapper = (DbClassMapper<TYPE>) mappers.get(theClass);
		if (mapper==null) {
			synchronized (mappers) {
				mapper = (DbClassMapper<TYPE>) mappers.get(theClass);
				if (mapper==null) {
					mapper = new DbClassMapper<TYPE>(this, theClass, timeZone);
					mappers.put(theClass, mapper);
				}
			}
		}
		return mapper;
	}

	/**
	 * Retourne le nom de la table mapp�e sur la classe.
	 * @param theClass la classe mapp�e.
	 * @return le nom de la table mapp�e sur la classe.
	 * @throws DbClassMappingException lev�e en cas de mauvaise configuration du
	 * 		mapping.
	 */
	public String getSQLTableName(Class<?> theClass) throws DbClassMappingException {
		String className = theClass.getName();
		int idx = className.lastIndexOf('.');
		String name = className.substring(idx + 1);
		String key = name + ".table";
		String tableName = props.getProperty(key);
		if (tableName==null)
			throw new DbClassMappingException("Nom de table pour la classe '" + name + "' non d�fini", null);
		return tableName;
	}

	/**
	 * Retourne le nom de la colonne mapp�e sur l'attribut de la classe.
	 * @param theClass la classe mapp�e.
	 * @param attribteName le nom de l'attribut.
	 * @return le nom de la colonne mapp�e sur l'attribut de la classe.
	 */
	public String getSQLColumnName(Class<?> theClass, String attributeName) {
		String className = theClass.getName();
		int idx = className.lastIndexOf('.');
		String name = className.substring(idx + 1);
		String key = name + "." + attributeName + ".column";
		String columnName = props.getProperty(key);
		return columnName;
	}

	/**
	 * Retourne la liste des attributs formant la cl� primaire de la classe mapp�e.
	 * @param theClass la classe mapp�e.
	 * @return la liste des attributs formant la cl� primaire de la classe mapp�e.
	 * @throws DbClassMappingException lev�e en cas de mauvaise configuration du
	 * 		mapping.
	 */
	public String[] getPrimaryKeyAttributesName(Class<?> theClass) throws DbClassMappingException {
		String className = theClass.getName();
		int idx = className.lastIndexOf('.');
		String name = className.substring(idx + 1);
		String key = name + ".PrimaryKey";
		String pkList = props.getProperty(key);
		if (pkList==null)
			throw new DbClassMappingException("Cl� primaire de la classe '" + name + "' non d�fini", null);
		String[] pkAtrributeNames = pkList.split(" *, *");
		return pkAtrributeNames;
	}

	/**
	 * Retourne l'�ventuel attribut auto g�n�r� par la base de donn�es.
	 * @param theClass la classe mapp�e.
	 * @return l'�ventuel attribut auto g�n�r� par la base de donn�es.
	 */
	public String getAutoGeneratedAttributeName(Class<?> theClass) {
		String className = theClass.getName();
		int idx = className.lastIndexOf('.');
		String name = className.substring(idx + 1);
		String key = name + ".AutoGenerated";
		String attributeName = props.getProperty(key);
		return attributeName;
	}

	/**
	 * Retourne le format de la colonne.
	 * @param theClass la classe mapp�e.
	 * @param attribteName le nom de l'attribut.
	 * @return le format de la colone.
	 */
	public String getAttributeFormat(Class<?> theClass, String attributeName) {
		String className = theClass.getName();
		int idx = className.lastIndexOf('.');
		String name = className.substring(idx + 1);
		String key = name + "." + attributeName + ".format";
		String format = props.getProperty(key);
		return format;
	}

}
