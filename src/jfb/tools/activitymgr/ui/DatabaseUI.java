/*
 * Copyright (c) 2004, Jean-Fran�ois Brazeau. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 * 
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 * 
 *  3. The name of the author may not be used to endorse or promote products
 *     derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIEDWARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jfb.tools.activitymgr.ui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.parsers.ParserConfigurationException;

import jfb.tools.activitymgr.core.DbException;
import jfb.tools.activitymgr.core.ModelException;
import jfb.tools.activitymgr.core.ModelMgr;
import jfb.tools.activitymgr.ui.util.CfgMgr;
import jfb.tools.activitymgr.ui.util.SafeRunner;
import jfb.tools.activitymgr.ui.util.UITechException;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.xml.sax.SAXException;

/**
 * IHM associ�e � l'onglet de param�trage de l'acc�s � la base de donn�es.
 */
public class DatabaseUI implements ModifyListener {

	/**
	 * Interface utilis�e pour notifier de l'�tat de la connexion
	 * � la base de donn�es.
	 */
	public static interface DbStatusListener {

		/**
		 * Notifie de l'ouverture de l'acc�s � la base de donn�es.
		 */
		public void databaseOpened();

		/**
		 * Notifie de l'ouverture de la fermeture.
		 */
		public void databaseClosed();

	}
	
	/** Logger */
	private static Logger log = Logger.getLogger(DatabaseUI.class);

	/** Constantes */
	public static final int STANDALONE_MODE = 0;
	public static final int MYSQL_SERVER_MODE = 1;
	public static final int USER_DEFINED_MODE = 2;
	
	/** Listener */
	private ArrayList listeners = new ArrayList();
	
	/** Composant parent */
	private Composite parent;

	/** Panneau contenant les controles */
	private Composite centeredPanel;

	/** Panneau contenant les donn�es de connexion � la BDD */
	private Composite conectionPanel;

	/** Panneau contenant les boutons d'export/import */
	private Composite xmlPanel;
	
	/** Champs de saisie, controles et labels */
	private Label dbTypeLabel;
	private Combo dbTypeCombo;
	private Label jdbcDriverLabel;
	private Text jdbcDriverText;
	private Label dbHostLabel;
	private Text dbHostText;
	private Label dbPortLabel;
	private Text dbPortText;
	private FileFieldEditor dbDataFileText;
	private Label dbNameLabel;
	private Text dbNameText;
	private Label jdbcUrlLabel;
	private Text jdbcUrlText;
	private Label jdbcUserIdLabel;
	private Text jdbcUserIdText;
	private Label jdbcPasswordLabel;
	private Text jdbcPasswordText;
	private Label jdbcPasswordWarning;
	private Button openDbButton;
	private Button closeDbButton;
	private Button resetDbDataButton;
	private FileFieldEditor xmlFileText;
	private Button xmlExportButton;
	private Button xmlImportButton;

	/**
	 * Constructeur permettant de placer l'IHM dans un onglet.
	 * @param tabItem item parent.
	 */
	public DatabaseUI(TabItem tabItem) {
		this(tabItem.getParent());
		tabItem.setControl(parent);
	}

	/**
	 * Constructeur par d�faut.
	 * @param parentComposite composant parent.
	 */
	public DatabaseUI(Composite parentComposite) {
		// Cr�ation du composite parent
		parent = new Composite(parentComposite, SWT.NONE);
		parent.setLayout(new GridLayout(1, false));
		centeredPanel = new Composite(parent, SWT.NONE);
		centeredPanel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		centeredPanel.setLayout(new GridLayout(1, false));

		// Groupe et pannneau contenant les donn�es de connexion � la BDD
		Group conectionGroup = new Group(centeredPanel, SWT.NONE);
		conectionGroup.setText("Connection properties");
		FillLayout fillLayout = new FillLayout(SWT.VERTICAL);
		fillLayout.marginWidth = 5;
		fillLayout.marginHeight = 5;
		conectionGroup.setLayout(fillLayout);
		conectionPanel = new Composite(conectionGroup, SWT.NONE);
		conectionPanel.setLayout(new GridLayout(3, false));
		
		// Type de BDD
		dbTypeLabel = new Label(conectionPanel, SWT.NONE);
		dbTypeLabel.setText("Database type :");
		dbTypeCombo = new Combo(conectionPanel, SWT.READ_ONLY);
		dbTypeCombo.add("Standalone mode (embedded HSQL database)");
		dbTypeCombo.add("MySQL Server database");
		dbTypeCombo.add("User defined database");
		dbTypeCombo.select(STANDALONE_MODE);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan = 2;
		dbTypeCombo.setLayoutData(gridData);
		dbTypeCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// Construction d'un contexte d'ex�cution s�curis�
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						dbTypeChanged();
						return null;
					}
				};
				// Ex�cution du traitement
				runner.run(parent.getShell());
			}
		});
		
		// Driver JDBC
		jdbcDriverLabel = new Label(conectionPanel, SWT.NONE);
		jdbcDriverLabel.setText("JDBC Driver :");
		jdbcDriverText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan = 2;
		jdbcDriverText.setLayoutData(gridData);

		// Nom d'h�te & port d'�coute de la BDD
		dbHostLabel = new Label(conectionPanel, SWT.NONE);
		dbHostLabel.setText("Database host :");
		Composite hostAndPortPanel = new Composite(conectionPanel, SWT.NONE);
		GridLayout layout = new GridLayout(3, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		hostAndPortPanel.setLayout(layout);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = SWT.FILL;
		hostAndPortPanel.setLayoutData(gridData);
		// Host
		dbHostText = new Text(hostAndPortPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.grabExcessHorizontalSpace = true;
		dbHostText.setLayoutData(gridData);
		// Port d'�coute de la BDD
		dbPortLabel = new Label(hostAndPortPanel, SWT.NONE);
		dbPortLabel.setText("Port :");
		dbPortText = new Text(hostAndPortPanel, SWT.BORDER);
		dbPortText.setText("XXXX");

		// Fichier de donn�es
		dbDataFileText = new FileFieldEditor("datafile", "Data file :", conectionPanel);

		// Nom de la BDD
		dbNameLabel = new Label(conectionPanel, SWT.NONE);
		dbNameLabel.setText("Database name :");
		dbNameText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.horizontalAlignment = SWT.FILL;
		gridData.horizontalSpan = 2;
		dbNameText.setLayoutData(gridData);

		// URL de connexion
		jdbcUrlLabel = new Label(conectionPanel, SWT.NONE);
		jdbcUrlLabel.setText("Server URL :");
		jdbcUrlText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 250;
		gridData.horizontalSpan = 2;
		jdbcUrlText.setLayoutData(gridData);

		// User de connexion
		jdbcUserIdLabel = new Label(conectionPanel, SWT.NONE);
		jdbcUserIdLabel.setText("User ID :");
		jdbcUserIdText = new Text(conectionPanel, SWT.BORDER);
		gridData = new GridData();
		gridData.widthHint = 80;
		gridData.horizontalSpan = 2;
		jdbcUserIdText.setLayoutData(gridData);
		
		// Password de connexion
		jdbcPasswordLabel = new Label(conectionPanel, SWT.NONE);
		jdbcPasswordLabel.setText("Password :");
		// Panneau contenant le champ + le warning
		Composite jdbcPasswordAndWarningPanel = new Composite(conectionPanel, SWT.NONE);
		layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		jdbcPasswordAndWarningPanel.setLayout(layout);
		gridData = new GridData();
		gridData.horizontalSpan = 2;
		gridData.horizontalAlignment = SWT.FILL;
		jdbcPasswordAndWarningPanel.setLayoutData(gridData);
		// Champ password
		jdbcPasswordText = new Text(jdbcPasswordAndWarningPanel, SWT.BORDER | SWT.PASSWORD);
		gridData = new GridData();
		gridData.widthHint = 80;
		jdbcPasswordText.setLayoutData(gridData);
		// Warning
		jdbcPasswordWarning = new Label(jdbcPasswordAndWarningPanel, SWT.NONE);
		jdbcPasswordWarning.setText("(password stored in plain text)");
		
		// Panneau contenant les boutons d'ouverture/fermeture de la BDD
		Composite openCloseDbButtonsPanel = new Composite(conectionPanel, SWT.NONE);
		openCloseDbButtonsPanel.setLayout(new GridLayout(3, false));
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gridData.horizontalSpan = 3;
		openCloseDbButtonsPanel.setLayoutData(gridData);

		// Bouton d'ouverture/fermeture de la BDD
		openDbButton = new Button(openCloseDbButtonsPanel, SWT.NONE);
		openDbButton.setText("Open database");
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		openDbButton.setLayoutData(gridData);
		openDbButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						openDatabase();
						return null;
					}
				};
				// Ex�cution du traitement
				runner.run(parent.getShell());
			}
		});
		closeDbButton = new Button(openCloseDbButtonsPanel, SWT.NONE);
		closeDbButton.setText("Close database");
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		closeDbButton.setLayoutData(gridData);
		closeDbButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						closeDatabase();
						return null;
					}
				};
				// Ex�cution du traitement
				runner.run(parent.getShell());
			}
		});
		// D�sactivation du bouton
		closeDbButton.setEnabled(false);

		// Bouton de r�installation de la base de donn�es
		resetDbDataButton = new Button(openCloseDbButtonsPanel, SWT.NONE);
		resetDbDataButton.setText("Reset database data");
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		resetDbDataButton.setLayoutData(gridData);
		resetDbDataButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						reinstallDatabaseWithWarnings();
						return null;
					}
				};
				// Ex�cution du traitement
				runner.run(parent.getShell());
			}
		});
		// D�sactivation du bouton
		resetDbDataButton.setEnabled(false);

		// Groupe et pannneau contenant les bouton d'export/import
		Group xmlGroup = new Group(centeredPanel, SWT.NONE);
		xmlGroup.setLayoutData(new GridData(SWT.FILL, SWT.NONE, false, false));
		xmlGroup.setText("Export/import");
		xmlGroup.setLayout(fillLayout);
		xmlPanel = new Composite(xmlGroup, SWT.NONE);
		xmlPanel.setLayout(new GridLayout(3, false));

		// Fichier de donn�es
		xmlFileText = new FileFieldEditor("xmlFile", "XML file :", xmlPanel);
		disableField(xmlFileText, xmlPanel);

		// Panneau contenant les boutons d'ouverture/fermeture de la BDD
		Composite xmlButtonsPanel = new Composite(xmlPanel, SWT.NONE);
		xmlButtonsPanel.setLayout(new GridLayout(2, false));
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		gridData.horizontalSpan = 3;
		xmlButtonsPanel.setLayoutData(gridData);

		// Bouton d'ouverture/fermeture de la BDD
		xmlExportButton = new Button(xmlButtonsPanel, SWT.NONE);
		xmlExportButton.setText("Export database");
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		xmlExportButton.setLayoutData(gridData);
		xmlExportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						exportToXML();
						return null;
					}
				};
				// Ex�cution du traitement
				runner.run(parent.getShell());
			}
		});
		disableField(xmlExportButton);
		xmlImportButton = new Button(xmlButtonsPanel, SWT.NONE);
		xmlImportButton.setText("Import from XML");
		gridData = new GridData(SWT.CENTER, SWT.CENTER, false, false);
		xmlImportButton.setLayoutData(gridData);
		xmlImportButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				SafeRunner runner = new SafeRunner() {
					public Object runUnsafe() throws Exception {
						importFromXML();
						return null;
					}
				};
				// Ex�cution du traitement
				runner.run(parent.getShell());
			}
		});
		// D�sactivation du bouton
		disableField(xmlImportButton);

	}

	/**
	 * Initialise l'IHM avec les donn�es en base.
	 */
	public void initUI() {
		// Valeurs par d�faut (� supprimer)
		int databaseType = 1;
		try { databaseType = Integer.parseInt(CfgMgr.get(CfgMgr.DATABASE_TYPE)); }
		catch (NumberFormatException ignored) { }
		String jdbcDriver = CfgMgr.get(CfgMgr.JDBC_DRIVER);
		String dbHost = CfgMgr.get(CfgMgr.DATABASE_HOST);
		String dbPort = CfgMgr.get(CfgMgr.DATABASE_PORT);
		String dbDataFile = CfgMgr.get(CfgMgr.DATABASE_DATA_FILE);
		String dbName = CfgMgr.get(CfgMgr.DATABASE_NAME);
		String jdbcUrl = CfgMgr.get(CfgMgr.JDBC_URL);
		String jdbcUser = CfgMgr.get(CfgMgr.JDBC_USER);
		String jdbcPassword = CfgMgr.get(CfgMgr.JDBC_PASSWORD);
		dbTypeCombo.select(databaseType);
		dbHostText.setText(dbHost!=null ? dbHost : "");
		dbPortText.setText(dbPort!=null ? dbPort : "");
		dbDataFileText.setStringValue(dbDataFile!=null ? dbDataFile : "");
		dbNameText.setText(dbName!=null ? dbName : "");
		jdbcDriverText.setText(jdbcDriver!=null ? jdbcDriver : "");
		jdbcUrlText.setText(jdbcUrl!=null ? jdbcUrl : "");
		jdbcUserIdText.setText(jdbcUser!=null ? jdbcUser : "");
		jdbcPasswordText.setText(jdbcPassword!=null ? jdbcPassword : "");
		// Mise � jour des donn�es
		dbTypeChanged();
	}
	
	/**
	 * M�thode invoqu�e lorsque l'utilisateur change le type de BDD dans l'IHM.
	 */
	protected void dbTypeChanged() {
		log.debug("dbTypeCombo.getSelectionIndex()=" + dbTypeCombo.getSelectionIndex());
		// D�sactivation de tout les champs
		disableField(jdbcDriverText);
		disableField(dbHostText);
		disableField(dbPortText);
		disableField(dbDataFileText, conectionPanel);
		disableField(dbNameText);
		disableField(jdbcUrlText);
		disableField(jdbcUserIdText);
		disableField(jdbcPasswordText);
		switch (dbTypeCombo.getSelectionIndex()) {
		// Cas d'une connexion JDBC HSQL embarqu�
		case STANDALONE_MODE :
			enabledField(dbDataFileText, conectionPanel, "data/activitymgr", false);
			break;
		// Cas d'une connexion MySQL
		case MYSQL_SERVER_MODE :
			enabledField(dbHostText, "localhost", false);
			enabledField(dbPortText, "3306", true);
			enabledField(dbNameText, "taskmgr_db", false);
			enabledField(jdbcUserIdText, "taskmgr_db", false);
			enabledField(jdbcPasswordText, "", false);
			break;
		// Cas d'une connexion autre
		case USER_DEFINED_MODE :
			enabledField(jdbcDriverText, "<jdbc driver>", false);
			enabledField(jdbcUrlText, "<jdbc url>", false);
			enabledField(jdbcUserIdText, "<jdbc_user_id>", false);
			enabledField(jdbcPasswordText, "", false);
			break;
		// Autre cas : erreur
		default :
			throw new Error("Unknown database type");
		}
		// Activation/d�sactivation des labels
		jdbcDriverLabel.setEnabled(jdbcDriverText.getEnabled());
		dbHostLabel.setEnabled(dbHostText.getEnabled());
		dbPortLabel.setEnabled(dbPortText.getEnabled());
		dbNameLabel.setEnabled(dbNameText.getEnabled());
		jdbcUrlLabel.setEnabled(jdbcUrlText.getEnabled());
		jdbcUserIdLabel.setEnabled(jdbcUserIdText.getEnabled());
		jdbcPasswordLabel.setEnabled(jdbcPasswordText.getEnabled());
		jdbcPasswordWarning.setEnabled(jdbcPasswordText.getEnabled());
		// Mise � jour des champs
		entriesChanged();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
	 */
	public void modifyText(ModifyEvent e) {
		entriesChanged();
	}

	/**
	 * D�sactive le champ sp�cifi�.
	 * @param field le champ � d�sactiver.
	 */
	private void disableField(Text field) {
		Text text = (Text) field;
		text.setEnabled(false);
		text.removeModifyListener(this);
	}

	/**
	 * D�sactive le champ sp�cifi�.
	 * @param field le champ � d�sactiver.
	 */
	private void disableField(Control field) {
		field.setEnabled(false);
	}
	
	/**
	 * D�sactive l'�diteur de nom de fichier.
	 * @param field le champ � d�sactiver.
	 * @param parent le composant parent.
	 */
	private void disableField(FileFieldEditor field, Composite parent) {
		FileFieldEditor fileFieldEditor = (FileFieldEditor) field;
		fileFieldEditor.setEnabled(false, parent);
		fileFieldEditor.setPropertyChangeListener(null);
	}

	/**
	 * Active l'�diteur de nom de fichier.
	 * @param field le champ � d�sactiver.
	 * @param defaultValue valeur par d�faut.
	 * @param forceDefaultValue bool�en indiquant si la valeur par d�faut doit
	 *   �tre forc�e m�me quand le champ a d�j� une valeur.
	 */
	private void enabledField(Text field, String defaultValue, boolean forceDefaultValue) {
		// Cas d'un textfield
		field.setEnabled(true);
		if (forceDefaultValue || "".equals(field.getText()))
			field.setText(defaultValue);
		field.addModifyListener(this);
	}

	/**
	 * Active le champ.
	 * @param field le champ � d�sactiver.
	 * @param parent le composant parent.
	 * @param defaultValue valeur par d�faut.
	 * @param forceDefaultValue bool�en indiquant si la valeur par d�faut doit
	 *   �tre forc�e m�me quand le champ a d�j� une valeur.
	 */
	private void enabledField(FileFieldEditor field, Composite parent, String defaultValue, boolean forceDefaultValue) {
		field.setEnabled(true, parent);
		if (forceDefaultValue || "".equals(field.getStringValue()))
			field.setStringValue(defaultValue);
		field.setPropertyChangeListener(new IPropertyChangeListener() {
			public void propertyChange(PropertyChangeEvent event) {
				entriesChanged();
			}
		});
	}

	/**
	 * R�agit � un changement des donn�es saisies par l'utilisateur.
	 */
	private void entriesChanged() {
		log.debug("Entries changed");
		switch (dbTypeCombo.getSelectionIndex()) {
		// Cas d'une connexion JDBC HSQL embarqu�
		case STANDALONE_MODE :
			jdbcDriverText.setText("org.hsqldb.jdbcDriver");
			dbHostText.setText("");
			dbPortText.setText("");
			dbNameText.setText("");
			jdbcUrlText.setText("jdbc:hsqldb:file:" + dbDataFileText.getStringValue());
			jdbcUserIdText.setText("sa");
			jdbcPasswordText.setText("");
			break;
		// Cas d'une connexion MySQL
		case MYSQL_SERVER_MODE :
			jdbcDriverText.setText("com.mysql.jdbc.Driver");
			dbDataFileText.setStringValue("");
			jdbcUrlText.setText("jdbc:mysql://" + dbHostText.getText() + ":" + dbPortText.getText() + "/" + dbNameText.getText());
			break;
		// Cas d'une connexion autre
		case USER_DEFINED_MODE :
			dbHostText.setText("");
			dbPortText.setText("");
			dbNameText.setText("");
			dbDataFileText.setStringValue("");
			break;
		// Autre cas : erreur
		default :
			throw new Error("Unknown database type");
		}
	}

	/**
	 * Ajoute un listener.
	 * @param listener le nouveau listener.
	 */
	public void addDbStatusListener(DbStatusListener listener) {
		listeners.add(listener);
	}

	/**
	 * Ajoute un listener.
	 * @param listener le nouveau listener.
	 */
	public void removeDbStatusListener(DbStatusListener listener) {
		listeners.remove(listener);
	}

	/**
	 * Ouvre la connexion � la base de donn�es.
	 * @throws IOException lev� en cas d'incident I/O lors du chargement de la
	 *    configuration.
	 * @throws DbException lev� en cas d'incident technique d'acc�s � la base.
	 * @throws UITechException lev� en cas d'incident inattendu lors de la cr�ation des dur�es.
	 */
	private void openDatabase() throws IOException, DbException, UITechException {
		// R�cup�ration des param�tres de connexion
		String databaseType = String.valueOf(dbTypeCombo.getSelectionIndex());
		String jdbcDriver = jdbcDriverText.getText().trim();
		String dbHost = dbHostText.getText().trim();
		String dbPort = dbPortText.getText().trim();
		String dbDataFile = dbDataFileText.getStringValue().trim();
		String dbName = dbNameText.getText().trim();
		String jdbcUrl = jdbcUrlText.getText().trim();
		String jdbcUser = jdbcUserIdText.getText().trim();
		String jdbcPassword = jdbcPasswordText.getText();
		
		// Sauvagarde dans le fichier de config
		CfgMgr.set(CfgMgr.DATABASE_TYPE, databaseType);
		CfgMgr.set(CfgMgr.JDBC_DRIVER, jdbcDriver);
		CfgMgr.set(CfgMgr.DATABASE_HOST, dbHost);
		CfgMgr.set(CfgMgr.DATABASE_PORT, dbPort);
		CfgMgr.set(CfgMgr.DATABASE_DATA_FILE, dbDataFile);
		CfgMgr.set(CfgMgr.DATABASE_NAME, dbName);
		CfgMgr.set(CfgMgr.JDBC_URL, jdbcUrl);
		CfgMgr.set(CfgMgr.JDBC_USER, jdbcUser);
		CfgMgr.set(CfgMgr.JDBC_PASSWORD, jdbcPassword);
		CfgMgr.save();
		
		// Changement des param�tres de connexion
		ModelMgr.initDatabaseAccess(
				jdbcDriver,
				jdbcUrl,
				jdbcUser,
				jdbcPassword
			);

		// Test de l'existence du mod�le en base
		boolean dbModelOk = ModelMgr.tablesExist();
		// Si le mod�le n'est pas install� et que l'utilisateur
		// le d�sire, l'application cr�e automatiquement les tables
		if (!dbModelOk) {
			if (MessageDialog.openConfirm(
					parent.getShell(), 
					"Confirmation", 
					"The database doesn't seem to be installed.\nWould you like to install it now ?")) {
				// Cr�ation des tables
				reinstallDatabase();
				dbModelOk = true;
			}
			else {
				MessageDialog.openError(
					parent.getShell(), 
					"Error",
					"Database not installed.\nConnection failed.");
			}
		}
		
		// Si le mod�le de donn�es est bien install�
		if (dbModelOk) {
			// Activation/d�sactivation des boutons et des champs
			disableField(dbTypeCombo);
			disableField(dbTypeLabel);
			disableField(jdbcDriverLabel);
			disableField(jdbcDriverText);
			disableField(dbHostLabel);
			disableField(dbHostText);
			disableField(dbPortLabel);
			disableField(dbPortText);
			disableField(dbDataFileText, conectionPanel);
			disableField(dbNameLabel);
			disableField(dbNameText);
			disableField(jdbcUrlLabel);
			disableField(jdbcUrlText);
			disableField(jdbcUserIdLabel);
			disableField(jdbcUserIdText);
			disableField(jdbcPasswordLabel);
			disableField(jdbcPasswordText);
			disableField(jdbcPasswordWarning);
			openDbButton.setEnabled(false);
			closeDbButton.setEnabled(true);
			resetDbDataButton.setEnabled(true);
			enabledField(xmlFileText, xmlPanel, "", false);
			xmlExportButton.setEnabled(true);
			xmlImportButton.setEnabled(true);

			// Notification de changement de statut de la connexion
			Iterator it = listeners.iterator();
			while (it.hasNext()) {
				DbStatusListener listener = (DbStatusListener) it.next();
				listener.databaseOpened();
			}
		}

	}

	/**
	 * Ferme la connexion � la base de donn�es.
	 * @throws DbException lev� en cas d'incident technique d'acc�s � la base.
	 */
	private void closeDatabase() throws DbException {
		// Changement des param�tres de connexion
		ModelMgr.closeDatabaseAccess();
		// Activation/d�sactivation des boutons et des champs
		openDbButton.setEnabled(true);
		closeDbButton.setEnabled(false);
		resetDbDataButton.setEnabled(false);
		dbTypeCombo.setEnabled(true);
		dbTypeLabel.setEnabled(true);
		disableField(xmlFileText, xmlPanel);
		xmlExportButton.setEnabled(false);
		xmlImportButton.setEnabled(false);
		dbTypeChanged();

		// Notification de changement de statut de la connexion
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			DbStatusListener listener = (DbStatusListener) it.next();
			listener.databaseClosed();
		}
	}

	/**
	 * R�installe la base de donn�es (tables drop + creation).
	 * @throws DbException lev� en cas d'incident technique d'acc�s � la base.
	 * @throws UITechException lev� en cas d'incident inattendu lors de la cr�ation des dur�es.
	 */
	private void reinstallDatabase() throws DbException, UITechException {
		// Suppression et recr�ation des tables
		ModelMgr.createTables();
		// Question concernant le r�f�rentiel de dur�es par d�faut 
		if (MessageDialog.openQuestion(
				parent.getShell(), 
				"Confirmation", 
				"Database tables initialization done.\n" +
				"Do you want me to create default durations (0.25, 0.50, 0.75 & 1.00) ?\n" +
				"Warning : \n" +
				"  - if you are about to import an XML file, choose no to avoid data conflicts.\n" +
				"  - if you choose no, you may have to create it manually.")) {
			try {
				ModelMgr.createDuration(25);
				ModelMgr.createDuration(50);
				ModelMgr.createDuration(75);
				ModelMgr.createDuration(100);
			}
			catch (ModelException e) {
				log.error("Unexpected error while creating default durations", e);
				throw new UITechException("Unexpected error while creating default durations", e);
			}
		}
		// Notification des listeners (reset �quivalent � r�ouverture de la BDD)
		Iterator it = listeners.iterator();
		while (it.hasNext()) {
			DbStatusListener listener = (DbStatusListener) it.next();
			listener.databaseOpened();
		}
	}
	
	/**
	 * R�installe la base de donn�es (tables drop + creation).
	 * @throws DbException lev� en cas d'incident technique d'acc�s � la base.
	 * @throws UITechException lev� en cas d'incident inattendu lors de la cr�ation des dur�es.
	 */
	private void reinstallDatabaseWithWarnings() throws DbException, UITechException {
		if (MessageDialog.openQuestion(
				parent.getShell(), 
				"Confirmation", 
				"Are you sure you want to reset the database data ?")) {
			if (MessageDialog.openQuestion(
					parent.getShell(), 
					"Confirmation", 
					"Really sure ???? (You may DEFINITELY loose your data)")) {
				reinstallDatabase();
			}
		}
	}
	
	/**
	 * Exporte le contenu de la BDD vers un fichier XML.
	 * @throws DbException lev� en cas d'incident technique d'acc�s � la base.
	 * @throws IOException lev� en cas d'incident I/O lors de l'�criture dans le fichier XML.
	 */
	private void exportToXML() throws DbException, IOException {
		String fileName = xmlFileText.getStringValue();
		if ("".equals(fileName.trim())) {
			MessageDialog.openWarning(
				parent.getShell(), 
				"File name error", 
				"XML file name not specified!");
		}
		else {
			File xmlFile = new File(fileName);
			if (!xmlFile.exists()
				|| MessageDialog.openConfirm(
					parent.getShell(), 
					"Confirmation", 
					"File exists. Overwrite ?")) {
				FileOutputStream out = new FileOutputStream(xmlFile);
				ModelMgr.exportToXML(out);
				out.close();
				// Popup d'info de fin de traitement
				MessageDialog.openInformation(
					parent.getShell(), 
					"Information", 
					"Database successfully exported.");
			}
		}
	}

	/**
	 * Importe les donn�es contenues dans un fichier XML.
	 * @throws IOException lev� en cas d'incident I/O lors de la lecture du fichier XML.
	 * @throws ParserConfigurationException lev� en cas de mauvaise configuration du parser XML.
	 * @throws SAXException lev� en cas de mauvais format du fichier XML.
	 * @throws ModelException lev� en cas de violation du mod�le de donn�es.
	 * @throws UITechException lev� en cas d'incident inattendu.
	 * @throws DbException lev� en cas d'incident technique d'acc�s � la base.
	 */
	private void importFromXML() throws IOException, ParserConfigurationException, SAXException, ModelException, UITechException, DbException {
		String fileName = xmlFileText.getStringValue();
		File xmlFile = new File(fileName);
		if ("".equals(fileName.trim())) {
			MessageDialog.openWarning(
				parent.getShell(), 
				"File name error", 
				"XML file name not specified!");
		}
		else if (!xmlFile.exists()) {
			MessageDialog.openWarning(
				parent.getShell(), 
				"File error", 
				"File doesn't exist. Please specify a valid file name.");
		}
		else {
			if (MessageDialog.openConfirm(
					parent.getShell(), 
					"Confirmation", 
					"Are you sure you want to perform this importation ?")) {
				// Peut-�tre l'utilisateur veut faire un reset sur la base
				// avant import
				if (MessageDialog.openQuestion(
						parent.getShell(), 
						"Confirmation", 
						"Do you want to reset the database tables before importing ?")) {
					// M�me traitement que pour le bouton 'Reset database data'
					reinstallDatabaseWithWarnings();
				}
				// Importation des donn�es
				FileInputStream in = new FileInputStream(xmlFile);
				ModelMgr.importFromXML(in);
				in.close();
				// Notification de fikn de chargement (�quivalent ouverture BDD)
				Iterator it = listeners.iterator();
				while (it.hasNext()) {
					DbStatusListener listener = (DbStatusListener) it.next();
					listener.databaseOpened();
				}
				// Popup d'info de fin de traitement
				MessageDialog.openInformation(
					parent.getShell(), 
					"Information", 
					"XML file successfully imported.");
			}
		}
	}

}
