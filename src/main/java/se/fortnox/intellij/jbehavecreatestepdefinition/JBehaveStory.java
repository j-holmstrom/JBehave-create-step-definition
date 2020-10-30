package se.fortnox.intellij.jbehavecreatestepdefinition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class JBehaveStory {
	private static final String GIVEN_PREFIX = "Given";
	private static final String WHEN_PREFIX  = "When";
	private static final String THEN_PREFIX  = "Then";

	private static final Set<String> GIVEN = new HashSet<>(Arrays.asList("given", "givet"));
	private static final Set<String> WHEN  = new HashSet<>(Arrays.asList("when", "när"));
	private static final Set<String> THEN  = new HashSet<>(Arrays.asList("then", "så"));
	private static final Set<String> AND   = new HashSet<>(Arrays.asList("and", "och"));

	private final List<JBehaveStep> steps;

	public JBehaveStory(List<JBehaveStep> steps) {
		this.steps = steps;
	}

	public static JBehaveStory fromText(String storyAsText) {
		List<String>      stepsAsStrings = Arrays.asList(storyAsText.split("\n"));
		List<JBehaveStep> steps          = new ArrayList<>();
		Optional<String>  lastType       = Optional.empty();
		for (String stepAsString : stepsAsStrings) {
			if (stepAsString.isEmpty()) {
				lastType = Optional.empty();
				continue;
			}
			int endOfPrefix = stepAsString.indexOf(" ");
			if (endOfPrefix == -1) {
				continue;
			}
			String lowercasePrefix = stepAsString.substring(0, stepAsString.indexOf(" ")).toLowerCase();
			String stepBody        = stepAsString.substring(endOfPrefix + 1);
			if (GIVEN.contains(lowercasePrefix)) {
				lastType = Optional.of(GIVEN_PREFIX);
				steps.add(new JBehaveStep(GIVEN_PREFIX, stepBody));
			} else if (WHEN.contains(lowercasePrefix)) {
				lastType = Optional.of(WHEN_PREFIX);
				steps.add(new JBehaveStep(WHEN_PREFIX, stepBody));
			} else if (THEN.contains(lowercasePrefix)) {
				lastType = Optional.of(THEN_PREFIX);
				steps.add(new JBehaveStep(THEN_PREFIX, stepBody));
			} else if (AND.contains(lowercasePrefix)) {
				String type = lastType.orElse(GIVEN_PREFIX);
				steps.add(new JBehaveStep(type, stepBody));
			} else if (stepAsString.contains("|")) {
				steps.get(steps.size() - 1).setIsTableStep(true);
			} else if (stepAsString.contains(":") && lastType.isPresent()) { //
				steps.get(steps.size() - 1).setIsMapStep(true);
			}
		}
		return new JBehaveStory(steps);
	}

	public List<JBehaveStep> getSteps() {
		return steps;
	}
}
