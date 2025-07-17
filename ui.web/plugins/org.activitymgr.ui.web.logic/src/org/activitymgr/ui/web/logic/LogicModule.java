package org.activitymgr.ui.web.logic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.sql.DataSource;

import org.activitymgr.core.dto.Collaborator;
import org.activitymgr.core.dto.Task;
import org.activitymgr.core.model.CoreModelModule;
import org.activitymgr.core.model.IModelMgr;
import org.activitymgr.core.model.ModelException;
import org.activitymgr.ui.web.logic.impl.CollaboratorsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.ContributionsCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.TasksCellLogicFatory;
import org.activitymgr.ui.web.logic.impl.internal.CollaboratorsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.ConfigurationImpl;
import org.activitymgr.ui.web.logic.impl.internal.ContributionsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.CopyTaskPathButtonLogic;
import org.activitymgr.ui.web.logic.impl.internal.DefaultConstraintsValidator;
import org.activitymgr.ui.web.logic.impl.internal.NewContributionTaskButtonLogic;
import org.activitymgr.ui.web.logic.impl.internal.ReportsTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.TasksTabLogicImpl;
import org.activitymgr.ui.web.logic.impl.internal.ThreadLocalizedDbTransactionProviderImpl;
import org.activitymgr.ui.web.logic.impl.internal.services.RESTServicesModule;
import org.activitymgr.ui.web.logic.spi.IAuthenticatorExtension;
import org.activitymgr.ui.web.logic.spi.ICollaboratorsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.IContributionsCellLogicFactory;
import org.activitymgr.ui.web.logic.spi.IFeatureAccessManager;
import org.activitymgr.ui.web.logic.spi.ITabButtonFactory;
import org.activitymgr.ui.web.logic.spi.ITabFactory;
import org.activitymgr.ui.web.logic.spi.ITaskCreationPatternHandler;
import org.activitymgr.ui.web.logic.spi.ITasksCellLogicFactory;
import org.apache.commons.dbcp.BasicDataSource;
import org.apache.log4j.PropertyConfigurator;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

public class LogicModule extends AbstractModule {

	private static final IFeatureAccessManager DEFAULT_FEATURE_ACCESS_MANAGER = new IFeatureAccessManager() {
		@Override
		public boolean hasAccessToTab(Collaborator collaborator, String tab) {
			return true;
		}

		@Override
		public boolean canUpdateContributions(Collaborator connected, Collaborator contributor) {
			return true;
		}

	};

	
	@Override
	protected void configure() {
		// Load configuration
		Properties props = new Properties();
		try {
			String installArea = new URL(
					System.getProperty("osgi.install.area")).getFile();
			if (!attempToLoadConfiguration(props, new File(installArea))) {
				attempToLoadConfiguration(
						props,
						new File(System.getProperty("activitymgr.config",
								System.getProperty("user.home"))));
			}
		} catch (MalformedURLException e) {
			throw new IllegalStateException(e);
		}

		// Configure log4j
		PropertyConfigurator.configure(props);

		// Bind configuration
		ConfigurationImpl cfg = new ConfigurationImpl(props);
		bind(IConfiguration.class).toInstance(cfg);

		// Install core module
		install(new CoreModelModule());
		
		// Create the datasource
		final ThreadLocalizedDbTransactionProviderImpl dbTxProvider = new ThreadLocalizedDbTransactionProviderImpl(createDataSource(cfg));
		bind(ThreadLocalizedDbTransactionProviderImpl.class).toInstance(dbTxProvider);
		bind(Connection.class).toProvider(() -> dbTxProvider.get().getTx());
		
		// Default SPI implementations
		bind(IFeatureAccessManager.class).toInstance(DEFAULT_FEATURE_ACCESS_MANAGER);
		bind(IAuthenticatorExtension.class).to(DefaultAuthenticatorExtension.class).in(Singleton.class);
		bind(ICollaboratorsCellLogicFactory.class).toInstance(new CollaboratorsCellLogicFatory());
		bind(IContributionsCellLogicFactory.class).toInstance(new ContributionsCellLogicFatory());
		bind(ITasksCellLogicFactory.class).toInstance(new TasksCellLogicFatory());
		
		// Install REST services
		install(new RESTServicesModule());

		// Bind tabs
		Multibinder<ITabFactory> tabsBinder = Multibinder.newSetBinder(binder(), ITabFactory.class);
		tabsBinder.addBinding().toInstance(
				new ITabFactory.Impl(20, IContributionsTabLogic.ID, "contributions_tab", ContributionsTabLogicImpl::new));
		tabsBinder.addBinding().toInstance(
				new ITabFactory.Impl(40, ITasksTabLogic.ID, "tasks_tab", TasksTabLogicImpl::new));
		tabsBinder.addBinding().toInstance(
				new ITabFactory.Impl(60, ICollaboratorsTabLogic.ID, "collaborators_tab", CollaboratorsTabLogicImpl::new));
		tabsBinder.addBinding().toInstance(
				new ITabFactory.Impl(65, IReportsTabLogic.MY_REPORTS_ID, "reports_tab", parent -> new ReportsTabLogicImpl(parent, false)));
		tabsBinder.addBinding().toInstance(
				new ITabFactory.Impl(70, IReportsTabLogic.ADVANCED_REPORTS_ID, "reports_tab", parent -> new ReportsTabLogicImpl(parent, true)));


		// Bind task creation pattern
		MapBinder<String, ITaskCreationPatternHandler> tcpBinder = MapBinder.newMapBinder(binder(), String.class, ITaskCreationPatternHandler.class);
		tcpBinder.addBinding("0_none").to(NoneTaskCreationPatternHandler.class);

		// Bind contribution tab buttons
		Multibinder<ITabButtonFactory<IContributionsTabLogic>> ctlBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {});
		ctlBinder.addBinding().toInstance(parent -> new NewContributionTaskButtonLogic(parent));

		Multibinder<ITabButtonFactory<ITasksTabLogic>> ttBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<>() {});
		ttBinder.addBinding().toInstance(parent -> new CopyTaskPathButtonLogic(parent));
		
		// Bind constraints validator
		Multibinder<IConstraintsValidator> cvBinder = Multibinder.newSetBinder(binder(), new TypeLiteral<IConstraintsValidator>() {});
		cvBinder.addBinding().to(DefaultConstraintsValidator.class);
	}
	
	private DataSource createDataSource(IConfiguration cfg) {
		BasicDataSource datasource = new BasicDataSource();
		IConfiguration jdbcCfg = cfg.getScoped("activitymgr.jdbc", null);
		datasource.setDriverClassName(jdbcCfg.get("driver", "com.mysql.jdbc.Driver"));
		datasource.setUrl(jdbcCfg.get("url", "jdbc:mysql://localhost:3306/taskmgr_db"));
		datasource.setUsername(jdbcCfg.get("user", "taskmgr"));
		datasource.setPassword(jdbcCfg.get("password", "taskmgr"));
		datasource.setDefaultAutoCommit(false);

		return datasource;
	}

	private boolean attempToLoadConfiguration(Properties props, File cfgFolder) {
		System.out.println("Trying to load configuration from " + cfgFolder.getAbsolutePath());
		if (cfgFolder.exists() && cfgFolder.isDirectory()) {
			try {
				File[] propFiles = cfgFolder.listFiles((dir, name) -> name.endsWith(".properties"));
				for (File propFile : propFiles) {
					System.out.println("Loading " + propFile.getAbsolutePath());
					props.load(new FileInputStream(propFile));
				}
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
			return !props.isEmpty();
		}
		return false;
	}

}


class DefaultAuthenticatorExtension implements IAuthenticatorExtension {
	
	@Inject
	private IModelMgr modelMgr;

	private IConfiguration passwords;

	@Inject
	public DefaultAuthenticatorExtension(IConfiguration cfg) {
		passwords = cfg.getScoped("users", "password");
	}

	@Override
	public boolean authenticate(String login, String password) {
		if (!passwords.isEmpty()) {
			String pwd = passwords.get(login);
			return pwd != null && pwd.equals(password)
					&& modelMgr.getCollaborator(login) != null;
		} else {
			// If there is no password configured, simply check that the user
			// exists in the database
			return modelMgr.getCollaborator(login) != null;
		}
	}

}

class NoneTaskCreationPatternHandler implements ITaskCreationPatternHandler {
	
	@Override
	public List<Task> handle(IUILogicContext context, Task newTask)
			throws ModelException {
		return Collections.emptyList();
	}

	@Override
	public String getLabel() {
		return "None";
	}
}
