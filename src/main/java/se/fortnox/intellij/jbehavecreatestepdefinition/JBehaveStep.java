package se.fortnox.intellij.jbehavecreatestepdefinition;

import org.apache.commons.lang.StringUtils;

import java.util.Objects;

public class JBehaveStep {
	private String  stepType;
	private String  stepText;
	private boolean isTableStep;
	private boolean isMapStep;

	public JBehaveStep(String stepType, String stepName) {
		this.stepType = stepType;
		this.stepText = stepName;
	}

	public String getStepBody() {
		return stepText;
	}

	public String getStepType() {
		return stepType;
	}

	public void setIsTableStep(boolean isTableStep) {
		this.isTableStep = isTableStep;
	}

	public boolean isTableStep() {
		return isTableStep;
	}

	public void setIsMapStep(boolean b) {
		this.isMapStep = true;
	}

	public boolean isMapStep() {
		return isMapStep;
	}

	public String getStepNameWithParameters() {
		if(isTableStep) {
			return stepText + " $table";
		} else if(isMapStep) {
			return stepText + " $map";
		}
		return stepText;
	}

	public String getStepParameters() {
		if(isTableStep) {
			return "ExamplesTable table";
		} else if(isMapStep) {
			return "Map<String, String> map";
		}
		return "";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		JBehaveStep that = (JBehaveStep)o;
		return isTableStep == that.isTableStep &&
			Objects.equals(stepType, that.stepType) &&
			Objects.equals(stepText, that.stepText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(stepType, stepText, isTableStep);
	}

	public String getSuggestedMethodName() {
		String   suggestedName = this.stepType.toLowerCase();
		String[] stepTextWords = this.stepText.replaceAll("[^a-zA-Z0-9 ]", "").split(" ");
		for (int i = 0; i < stepTextWords.length; i++) {
			String word = stepTextWords[i];
			if (StringUtils.isNotBlank(word)) {
				suggestedName += word.substring(0, 1).toUpperCase() + word.substring(1);
			}
		}
		return suggestedName;
	}


}
