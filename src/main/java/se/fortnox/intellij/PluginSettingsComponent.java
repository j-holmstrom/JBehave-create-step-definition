package se.fortnox.intellij;

import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Supports creating and managing a {@link JPanel} for the Settings Dialog.
 */
public class PluginSettingsComponent {

	private final JPanel      panel;
	private final JBTextField tenantIdText = new JBTextField();
	private final JBTextField  userIdText   = new JBTextField();

	public PluginSettingsComponent() {
		panel = FormBuilder.createFormBuilder()
			.addLabeledComponent(new JBLabel("Default tenant id: "), tenantIdText, 1, false)
			.addLabeledComponent(new JBLabel("Default user id: "), userIdText, 1, false)
			.addComponentFillVertically(new JPanel(), 0)
			.getPanel();
	}

	public JPanel getPanel() {
		return panel;
	}

	public JComponent getPreferredFocusedComponent() {
		return tenantIdText;
	}

	@NotNull
	public String getTenantIdText() {
		return tenantIdText.getText();
	}

	public void setTenantIdText(@NotNull String newText) {
		tenantIdText.setText(newText);
	}

	@NotNull
	public String getUserIdText() {
		return userIdText.getText();
	}

	public void setUserIdText(@NotNull String newText) {
		userIdText.setText(newText);
	}

}