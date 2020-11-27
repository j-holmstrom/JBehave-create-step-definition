package se.fortnox.intellij.internalauthhelper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jetbrains.annotations.NotNull;
import se.fortnox.intellij.PluginSettingsState;

import java.io.IOException;
import java.util.Optional;

public class GenerateAuthIntentionProd extends PsiElementBaseIntentionAction implements IntentionAction {

	private PluginSettingsState pluginSettingsState = PluginSettingsState.getInstance();

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
		String currentFileExtension = element.getContainingFile()
			.getVirtualFile()
			.getExtension();
		return "http".equals(currentFileExtension);
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
		//Logga in på prod-pod-status
		Optional<String> kubeSessionCookie = doLoginAndGetCookie();
		if (!kubeSessionCookie.isPresent()) {
			return;
		}

		//Request list of pods and find uri to an active auth service pod
		Optional<String> uriToAuthPod = findActiveAuthPodUri(kubeSessionCookie.get());
		if (!uriToAuthPod.isPresent()) {
			return;
		}

		//Call auth service and generate a String containing the Auth in the format needed for .http files
		Optional<String> authAsString = callAuthAndGetAuthString(uriToAuthPod.get(), kubeSessionCookie.get());
		if (!authAsString.isPresent()) {
			return;
		}

		//Write the result into the document at the caret position
		WriteCommandAction.runWriteCommandAction(project, () -> {
			editor.getDocument()
				.insertString(editor.getCaretModel().getOffset(), authAsString.get());
		});
	}

	private Optional<String> callAuthAndGetAuthString(String uriToPod, String cookie) {
		String   requestUrl = String.format("%s/internalapi/auth/authinfo-v1/tenant/%s/user/%s", uriToPod, pluginSettingsState.tenantId, pluginSettingsState.userId);
		HttpPost httpPost   = new HttpPost(requestUrl);
		httpPost.addHeader("Cookie", cookie);
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			CloseableHttpResponse response       = client.execute(httpPost);
			String                responseString = EntityUtils.toString(response.getEntity(), "UTF-8");

			JsonNode contentAsJson = new ObjectMapper().readTree(responseString);
			String headers = String.format(
				"Cookie: %s%nX-TOKEN: %s%nHost: %s%nUser-Agent: %s%n",
				contentAsJson.findValue("Cookie").textValue(),
				contentAsJson.findValue("X-TOKEN").textValue(),
				contentAsJson.findValue("Host").textValue(),
				contentAsJson.findValue("User-Agent").textValue()
			);

			return Optional.of(headers);
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	private Optional<String> findActiveAuthPodUri(String cookie) {
		HttpPost httpPost = new HttpPost("https://prod-rke-pod-status.fnox.se/page/pods.html");
		httpPost.addHeader("Cookie", cookie);

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			CloseableHttpResponse response            = client.execute(httpPost);
			String                responseString      = EntityUtils.toString(response.getEntity(), "UTF-8");
			int                   indexOfFirstAuthPod = responseString.indexOf("authservice-");
			String                authPod             = responseString.substring(indexOfFirstAuthPod, indexOfFirstAuthPod + 28);

			String uriToAuthPod = String.format("https://prod-rke-pod-status.fnox.se/pods/default/%s/8080", authPod);
			return Optional.of(uriToAuthPod);
		} catch (IOException e) {
			return Optional.empty();
		}
	}

	private Optional<String> doLoginAndGetCookie() {
		HttpPost httpPost = new HttpPost("https://prod-rke-pod-status.fnox.se/page/pods.html/login");
		HttpEntity entity = MultipartEntityBuilder.create()
			.setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
			.addTextBody("username", "jesper.holmstrom")
			.addTextBody("password", "JagHeterJesper")
			.build();
		httpPost.setEntity(entity);
		RequestConfig requestConfig = RequestConfig.custom()
			.setRedirectsEnabled(false)
			.build();
		httpPost.setConfig(requestConfig);

		try (CloseableHttpClient client = HttpClients.createDefault()) {
			CloseableHttpResponse response = client.execute(httpPost);
			String setCookieHeader   = response.getHeaders("Set-Cookie")[0].toString();
			String kubeSessionCookie = setCookieHeader.substring("Set-Cookie: ".length());
			return Optional.of(kubeSessionCookie);

		} catch (IOException | IndexOutOfBoundsException e) {
			return Optional.empty();
		}
	}

	@Override
	public String getText() {
		return "PROD: Generate auth headers";
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
