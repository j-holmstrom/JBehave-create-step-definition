package se.fortnox.intellij.jbehavecreatestepdefinition;

import com.google.common.base.CaseFormat;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiInvalidElementAccessException;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.intellij.openapi.module.ModuleUtilCore.findModuleForFile;
import static java.lang.String.format;
import static java.util.Collections.emptyList;

public class JBehaveCreateStepIntention extends PsiElementBaseIntentionAction implements IntentionAction {

	private static final String STEP_TEMPLATE = "@%s(\"%s\")\npublic void %s (%s)\n{\n\t//Not implemented\n}";

	@NotNull
	@Override
	public String getText() {
		return "Create JBehave step definition";
	}

	@NotNull
	@Override
	public String getFamilyName() {
		return "CreateJBehaveStepDefinition";
	}

	@Override
	public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
		String currentFileExtension = element.getContainingFile().getVirtualFile().getExtension();
		return "story".equals(currentFileExtension);
	}

	@Override
	public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement element) {
		//Parse step
		Optional<JBehaveStep> step = parseJBehaveStep(element, editor);
		if (!step.isPresent()) {
			return;
		}

		//Find possible step classes where we can put our step implementation
		VirtualFile  storyFile   = element.getContainingFile().getVirtualFile();
		List<String> stepClasses = getPossibleStepImplementationClassNames(project, storyFile);
		if (stepClasses.isEmpty()) {
			return;
		}

		//Let user pick which class to insert the step implementation in
		JBPopup jbPopup = JBPopupFactory.getInstance().createPopupChooserBuilder(stepClasses)
			.setTitle("Pick Stepfile")
			.setItemChosenCallback(stepClassName -> createStepDefinitionInClass(project, step.get(), stepClassName))
			.createPopup();
		jbPopup.showInBestPositionFor(editor);
	}

	private Optional<JBehaveStep> parseJBehaveStep(PsiElement element, Editor editor) {
		try {
			String       storyFileText            = element.getContainingFile().getText();
			String       untilCursor              = storyFileText.substring(0, editor.getCaretModel().getOffset());
			String       fromCursorIncludingStart = storyFileText.substring(untilCursor.lastIndexOf("\n") + 1);
			String       wholeStep                = fromCursorIncludingStart.substring(0, fromCursorIncludingStart.indexOf("\n"));
			String       stepBody                 = StringUtil.trim(wholeStep.substring(wholeStep.indexOf(" ")));
			JBehaveStory story                    = JBehaveStory.fromText(storyFileText);
			Optional<JBehaveStep> optionalStep = story.getSteps()
				.stream()
				.filter(s -> s.getStepBody().equals(stepBody))
				.findFirst();
			if (!optionalStep.isPresent() || optionalStep.get().getStepType() == null || optionalStep.get().getStepBody() == null) {
				JBehaveErrorNotifier.notify("Failed to parse step. It is probably not a valid JBehave step.");
				return Optional.empty();
			}
			return optionalStep;
		} catch (IndexOutOfBoundsException | PsiInvalidElementAccessException e) {
			return Optional.empty();
		}
	}

	private void createStepDefinitionInClass(Project project, JBehaveStep step, String stepClassName) {
		PsiMethod stepDefinitionMethod = createStepDefinitionMethod(project, step);
		addMethodToClass(project, stepClassName, stepDefinitionMethod);
	}

	private void addMethodToClass(Project project, String stepClassName, PsiMethod stepDefinitionMethod) {
		Optional<PsiClass> mainClass = getStepImplementationClass(project, stepClassName);
		if (!mainClass.isPresent()) {
			return;
		}

		WriteCommandAction.runWriteCommandAction(project, () -> {
			mainClass.get().add(stepDefinitionMethod);
		});
	}

	@NotNull
	private PsiMethod createStepDefinitionMethod(Project project, JBehaveStep step) {
		final PsiElementFactory factory     = JavaPsiFacade.getInstance(project).getElementFactory();
		final CodeStyleManager  codeStylist = CodeStyleManager.getInstance(project);
		return (PsiMethod)codeStylist.reformat(
			factory.createMethodFromText(
				format(STEP_TEMPLATE,
					step.getStepType(),
					step.getStepNameWithParameters(),
					step.getSuggestedMethodName(),
					step.getStepParameters()),
				null
			)
		);
	}

	private Optional<PsiClass> getStepImplementationClass(Project project, String stepsClassName) {
		FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
		VirtualFile[]     selectedFiles     = fileEditorManager.getSelectedFiles();
		VirtualFile       file              = selectedFiles[0];

		try {
			Module    module   = findModuleForFile(file, project);
			PsiFile[] psiFiles = FilenameIndex.getFilesByName(project, stepsClassName.replace(".class", ".java"), GlobalSearchScope.moduleScope(module));
			return Optional.of(((PsiJavaFile)psiFiles[0]).getClasses()[0]);

		} catch (Exception e) {
			JBehaveErrorNotifier.notify("Could not find steps definition file named " + stepsClassName);
			return Optional.empty();
		}
	}

	private List<String> getPossibleStepImplementationClassNames(Project project, VirtualFile storyFile) {
		try {
			PsiFile classFile = getStoryClassfile(project, storyFile);
			if (classFile == null) {
				return emptyList();
			}

			String storyClassContent      = classFile.getContainingFile().getText();
			int    startOfSuper           = storyClassContent.indexOf("super(") + 6;
			String tmp                    = storyClassContent.substring(startOfSuper);
			String storyClassSuperContent = tmp.substring(0, tmp.indexOf(")"));
			List<String> possibleStepImplementationClassNames = Arrays.stream(storyClassSuperContent
				.split(","))
				.filter(argument -> argument.endsWith(".class"))
				.map(classFileName -> classFileName.replace(".class", ".java").trim())
				.collect(Collectors.toList());

			if (possibleStepImplementationClassNames.isEmpty()) {
				JBehaveErrorNotifier.notify("Could not find any valid step classes. This plugin can only handle step classes added to the super method call right now.");
			}
			return possibleStepImplementationClassNames;
		} catch (Exception ex) {
			JBehaveErrorNotifier.notify("Found story class but could not find any step classes in the super method call.");
			return emptyList();
		}
	}

	private PsiFile getStoryClassfile(Project project, VirtualFile storyFile) {
		Module module = findModuleForFile(storyFile, project);
		if (module == null) {
			JBehaveErrorNotifier.notify(format("Could not find module for storyfile %s", storyFile.getName()));
			return null;
		}
		String      path      = storyFile.getParent().getPath().replace("/src/test/resources", "/src/test/java");
		String      className = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, storyFile.getName().replace(".story", ".java"));
		String      filePath  = path + "/" + className;
		VirtualFile classFile = LocalFileSystem.getInstance().findFileByPath(filePath);
		if (classFile == null) {
			JBehaveErrorNotifier.notify(String.format("Could not find class file %s ", filePath));
			return null;
		}
		PsiFile psiStoryClassFile = PsiManager.getInstance(project).findFile(classFile);

		if (psiStoryClassFile == null) {
			JBehaveErrorNotifier.notify("Could not find any step classes");
		}
		return psiStoryClassFile;
	}

	@Override
	public boolean startInWriteAction() {
		return true;
	}
}
