package se.fortnox.intellij.internalauthhelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import se.fortnox.intellij.PluginSettingsComponent;
import se.fortnox.intellij.PluginSettingsState;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GenerateAuthIntention extends PsiElementBaseIntentionAction implements IntentionAction {

	private PluginSettingsState pluginSettingsState = PluginSettingsState.getInstance();

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
		String currentFileExtension = element.getContainingFile()
			.getVirtualFile()
			.getExtension();
		return "http".equals(currentFileExtension);
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) throws IncorrectOperationException {
		//Skapa en auth
		HttpURLConnection con = null;
		try {
			URL url = new URL(String.format("http://route-utv.fnox.se/internalapi/auth/authinfo-v1/tenant/%s/user/%s", pluginSettingsState.tenantId, pluginSettingsState.userId));
			con = (HttpURLConnection)url.openConnection();
			con.setRequestMethod("POST");
			BufferedReader in = new BufferedReader(
				new InputStreamReader(con.getInputStream()));
			String        inputLine;
			StringBuilder content = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				content.append(inputLine);
			}

			JsonNode contentAsJson = new ObjectMapper().readTree(content.toString());
			String headers = String.format(
				"Cookie: %s%nX-TOKEN: %s%nHost: %s%nUser-Agent: %s%n",
				contentAsJson.findValue("Cookie").textValue(),
				contentAsJson.findValue("X-TOKEN").textValue(),
				contentAsJson.findValue("Host").textValue(),
				contentAsJson.findValue("User-Agent").textValue()
			);
			WriteCommandAction.runWriteCommandAction(project, () -> {
				editor.getDocument()
					.insertString(editor.getCaretModel().getOffset(), headers); //lägg in svaret
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

		//formatera svaret


		System.out.println("In invoke");
		return;
	}

	@Override
	public String getText() {
		return "DEV: Generate auth headers";
	}

	@Override
	public String getFamilyName() {
		return "GenerateAuth";
	}

	@Override
	public boolean startInWriteAction() {
		return true;
	}
}
