package se.fortnox.intellij;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
	name = "se.fortnox.intellij.PluginState",
	storages = {@Storage("SdkSettingsPlugin.xml")}
)
public class PluginSettingsState implements PersistentStateComponent<PluginSettingsState> {

	public String tenantId = "";
	public String userId   = "";

	public static PluginSettingsState getInstance() {
		return ServiceManager.getService(PluginSettingsState.class);
	}

	@Nullable
	@Override
	public PluginSettingsState getState() {
		return this;
	}

	@Override
	public void loadState(@NotNull PluginSettingsState state) {
		XmlSerializerUtil.copyBean(state, this);
	}

}
