package org.activitymgr.ui.web.view.impl.internal;

import org.activitymgr.ui.web.logic.IAuthenticationLogic;
import org.activitymgr.ui.web.view.IResourceCache;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.google.inject.Inject;
import com.vaadin.event.ShortcutListener;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class AuthenticationPanel extends VerticalLayout
		implements IAuthenticationLogic.View {

	private IAuthenticationLogic logic;
	
	protected GridLayout formPanel;
	protected TextField userField;
	protected PasswordField passwordField;
	protected CheckBox rememberMeCheckBox;
	
	
	protected final IResourceCache resourceCache;
	
	@Inject
	public AuthenticationPanel(IResourceCache resourceCache) {
		this.resourceCache = resourceCache;
		
		setDefaultComponentAlignment(Alignment.MIDDLE_CENTER);

		setSizeFull();
		setSpacing(true);
		setMargin(true);

		Component header = createHeader();
		addComponent(header);
		setExpandRatio(header, 100);
		
		Component formPanel = createForm();
		addComponent(formPanel);
		setExpandRatio(formPanel, 100);
				

		Component bottom = createBottom();
		addComponent(bottom);
		setExpandRatio(bottom, 100);
		
		// Default focus management
		userField.focus();
		
		// Register the attach listener
		addAttachListener(evt -> logic.onViewAttached());
	}
	
	protected void applyLogin() {
		logic.onAuthenticate(userField.getValue(),
				passwordField.getValue(), 
				rememberMeCheckBox.getValue());
	}
	
	protected Component createForm() {
		// Form panel
		formPanel = new GridLayout(3, 5);
		formPanel.setMargin(true);
		formPanel.setSpacing(true);

		// setComponentAlignment(formPanel, Alignment.MIDDLE_CENTER);
		
		/* Line 1 */

		// User field
		Label userLabel = new Label("User");
		formPanel.addComponent(userLabel);
		formPanel.setComponentAlignment(userLabel, Alignment.MIDDLE_RIGHT);
		userField = new TextField();
		formPanel.addComponent(userField);
		formPanel.addComponent(new Label(""));
		
		/* Line 2 */

		// Password field
		Label passwordLabel = new Label("Password");
		formPanel.addComponent(passwordLabel);
		formPanel.setComponentAlignment(passwordLabel, Alignment.MIDDLE_RIGHT);
		passwordField = new PasswordField();
		formPanel.addComponent(passwordField);
		passwordField.addShortcutListener(new ShortcutListener("Authenticate", KeyCode.ENTER, null) {
			@Override
			public void handleAction(Object sender, Object target) {
				applyLogin();
			}
		});
		
		// Button
		Button validateButton = new Button("Login");
		// Register listeners
		validateButton.addClickListener(event -> applyLogin());

		formPanel.addComponent(validateButton);
		
		/* Line 3 */

		// Remember me
		formPanel.addComponent(new Label(""));
		rememberMeCheckBox = new CheckBox("Remember me");
		formPanel.addComponent(rememberMeCheckBox);
		formPanel.addComponent(new Label(""));
		return formPanel;

	}
	
	protected Component createHeader() {
		Image logo = new Image(null, resourceCache.getResource("Hour_work2.png"));
		logo.setAlternateText("Activity Manager");

		return logo;	
	}
	
	protected Component createBottom() {
		return new Label("");	
	}

	
	@Override
	public void registerLogic(IAuthenticationLogic logic) {
		this.logic = logic;
	}

	@Override
	public void setGoogleSignInClientId(String googleSignInClientId) {
		formPanel.addComponent(new Label(""));
		SignWithGoogleButton signInWithGoogleButton = new SignWithGoogleButton(
				googleSignInClientId);
		formPanel.addComponent(signInWithGoogleButton, 1, 3, 2, 3);

		// Register listeners
		signInWithGoogleButton.addListener((SignWithGoogleButton.Listener) idToken -> logic.onAuthenticateWithGoogle(idToken));
	}

}
