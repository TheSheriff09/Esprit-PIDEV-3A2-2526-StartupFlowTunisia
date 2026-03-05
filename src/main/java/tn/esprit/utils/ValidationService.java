package tn.esprit.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

// NEW FILE
public class ValidationService<T> {
    private final List<Rule<T>> rules = new ArrayList<>();

    public void addRule(Predicate<T> condition, String errorMessage) {
        rules.add(new Rule<>(condition, errorMessage));
    }

    public List<String> validate(T object) {
        List<String> errors = new ArrayList<>();
        for (Rule<T> rule : rules) {
            if (!rule.condition.test(object)) {
                errors.add(rule.errorMessage);
            }
        }
        return errors;
    }

    private static class Rule<T> {
        Predicate<T> condition;
        String errorMessage;

        Rule(Predicate<T> condition, String errorMessage) {
            this.condition = condition;
            this.errorMessage = errorMessage;
        }
    }
}
