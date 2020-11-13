package se.fortnox.intellij;

import com.intellij.openapi.options.Configurable;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Provides controller functionality for application settings.
 */
public class PluginSettingsConfigurable implements Configurable {

	private PluginSettingsComponent settingsComponent;

	// A default constructor with no arguments is required because this implementation
	// is registered as an applicationConfigurable EP

	@Nls(capitalization = Nls.Capitalization.Title)
	@Override
	public String getDisplayName() {
		return "Fortnox Utility Settings";
	}

	@Override
	public JComponent getPreferredFocusedComponent() {
		return settingsComponent.getPreferredFocusedComponent();
	}

	@Nullable
	@Override
	public JComponent createComponent() {
		settingsComponent = new PluginSettingsComponent();
		return settingsComponent.getPanel();
	}

	@Override
	public boolean isModified() {
		PluginSettingsState settings = PluginSettingsState.getInstance();
		boolean modified = !settingsComponent.getTenantIdText().equals(settings.tenantId);
		modified |= settingsComponent.getUserIdText().equals(settings.userId);
		return modified;
	}

	@Override
	public void apply() {
		PluginSettingsState settings = PluginSettingsState.getInstance();
		settings.tenantId = settingsComponent.getTenantIdText();
		settings.userId = settingsComponent.getUserIdText();
	}

	@Override
	public void reset() {
		PluginSettingsState settings = PluginSettingsState.getInstance();
		settingsComponent.setTenantIdText(settings.tenantId);
		settingsComponent.setUserIdText(settings.userId);
	}

	@Override
	public void disposeUIResources() {
		settingsComponent = null;
	}

}
