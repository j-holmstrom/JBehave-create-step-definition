package se.fortnox.intellij.jbehavecreatestepdefinition;

import junit.framework.TestCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JBehaveStepTest extends TestCase {

	@Test
	public void shouldReplaceDiacriticCharactersWithNonDiacriticCharacters() {
		JBehaveStep step = new JBehaveStep("Given", "åäö");
		assertThat(step.getSuggestedMethodName()).isEqualTo("givenAao");
	}
}