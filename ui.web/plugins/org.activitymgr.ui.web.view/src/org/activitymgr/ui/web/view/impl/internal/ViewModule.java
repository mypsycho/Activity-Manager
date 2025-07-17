package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.logic.ICheckBoxFieldLogic;
import org.activitymgr.ui.web.logic.ICollaboratorsTabLogic;
import org.activitymgr.ui.web.logic.IContributionTaskChooserLogic;
import org.activitymgr.ui.web.logic.IContributionsTabLogic;
import org.activitymgr.ui.web.logic.ICopyButtonLogic;
import org.activitymgr.ui.web.logic.IDownloadButtonLogic;
import org.activitymgr.ui.web.logic.IExternalContentDialogLogic;
import org.activitymgr.ui.web.logic.ILabelLogic;
import org.activitymgr.ui.web.logic.ILinkLogic;
import org.activitymgr.ui.web.logic.IReportsLogic;
import org.activitymgr.ui.web.logic.IReportsTabLogic;
import org.activitymgr.ui.web.logic.ISelectFieldLogic;
import org.activitymgr.ui.web.logic.IStandardButtonLogic;
import org.activitymgr.ui.web.logic.ITabFolderLogic;
import org.activitymgr.ui.web.logic.ITaskChooserLogic;
import org.activitymgr.ui.web.logic.ITasksTabLogic;
import org.activitymgr.ui.web.logic.ITextFieldLogic;
import org.activitymgr.ui.web.logic.ITwinSelectFieldLogic;
import org.activitymgr.ui.web.logic.LogicModule;
import org.activitymgr.ui.web.view.IResourceCache;
import org.activitymgr.ui.web.view.impl.dialogs.ExternalContentDialog;
import org.activitymgr.ui.web.view.impl.dialogs.TaskChooserDialog;
import org.activitymgr.ui.web.view.impl.internal.util.CheckBoxView;
import org.activitymgr.ui.web.view.impl.internal.util.CopyButtonView;
import org.activitymgr.ui.web.view.impl.internal.util.DownloadButtonView;
import org.activitymgr.ui.web.view.impl.internal.util.LabelView;
import org.activitymgr.ui.web.view.impl.internal.util.LinkView;
import org.activitymgr.ui.web.view.impl.internal.util.SelectFieldView;
import org.activitymgr.ui.web.view.impl.internal.util.StandardButtonView;
import org.activitymgr.ui.web.view.impl.internal.util.TextFieldView;
import org.activitymgr.ui.web.view.impl.internal.util.TwinSelectView;

import com.google.inject.AbstractModule;

public class ViewModule extends AbstractModule {

	@Override
	protected void configure() {
		// Bind logic module
		install(new LogicModule());

		// Resource cache
		bind(IResourceCache.class).toInstance(new ResourceCacheImpl());
		
		// Bind views
		bind(IAuthenticationLogic.View.class).to(AuthenticationPanel.class);
		bind(IContributionsTabLogic.View.class).to(ContributionsPanel.class);
		bind(ICollaboratorsTabLogic.View.class).to(CollaboratorsPanel.class);
		bind(ITasksTabLogic.View.class).to(TasksPanel.class);
		bind(IReportsTabLogic.View.class).to(ReportsTabPanel.class);
		bind(IReportsLogic.View.class).to(ReportsPanel.class);
		bind(ITaskChooserLogic.View.class).to(TaskChooserDialog.class);
		bind(IContributionTaskChooserLogic.View.class).to(ContributionTaskChooserDialog.class);
		bind(ILabelLogic.View.class).to(LabelView.class);
		bind(ILinkLogic.View.class).to(LinkView.class);
		bind(ITextFieldLogic.View.class).to(TextFieldView.class);
		bind(ICheckBoxFieldLogic.View.class).to(CheckBoxView.class);
		bind(ITabFolderLogic.View.class).to(TabFolderViewImpl.class);
		bind(IStandardButtonLogic.View.class).to(StandardButtonView.class);
		bind(IDownloadButtonLogic.View.class).to(DownloadButtonView.class);
		bind(ICopyButtonLogic.View.class).to(CopyButtonView.class);

		bind(ISelectFieldLogic.View.class).to(SelectFieldView.class);
		bind(ITwinSelectFieldLogic.View.class).to(TwinSelectView.class);
		bind(IExternalContentDialogLogic.View.class).to(
				ExternalContentDialog.class);
	}

}
