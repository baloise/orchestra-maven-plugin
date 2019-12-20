package com.baloise.os.webservice.handler;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.MessageInterpolator;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import javax.validation.metadata.ConstraintDescriptor;

public class ValidationHandler {

	ValidatorFactory VALIDATOR_FACTORY = Validation.buildDefaultValidatorFactory();
	Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();
	MessageInterpolator messageInterpolator = VALIDATOR_FACTORY.getMessageInterpolator();

	public void validate(Object... objectList) {
		Set<ConstraintViolation<Object>> violations = new HashSet<ConstraintViolation<Object>>();
		for (Object current : objectList) {
			violations.addAll(VALIDATOR.validate(current));
		}
		if (!violations.isEmpty()) {
			StringBuilder sb = new StringBuilder("Violations occurred");
			sb.append('\n');
			for (ConstraintViolation<Object> violation : violations) {
				sb.append(toMessage(violation));
				sb.append('\n');
			}
			throw new RuntimeException(sb.toString());
		}
	}

	private String toMessage(ConstraintViolation<?> violation) {
		MessageInterpolatorContext context = new MessageInterpolatorContext(violation.getConstraintDescriptor(), violation.getInvalidValue());
		String propertyPath = violation.getRootBeanClass().getSimpleName() + "." + violation.getPropertyPath() + " ";
		return propertyPath + messageInterpolator.interpolate(violation.getMessageTemplate(), context, Locale.ENGLISH);
	}

	private static final class MessageInterpolatorContext implements MessageInterpolator.Context {
		private final ConstraintDescriptor<?> constraintDescriptor;
		private final Object validatedValue;

		MessageInterpolatorContext(ConstraintDescriptor<?> constraintDescriptor, Object validatedValue) {
			this.constraintDescriptor = constraintDescriptor;
			this.validatedValue = validatedValue;
		}

		@Override
		public ConstraintDescriptor<?> getConstraintDescriptor() {
			return constraintDescriptor;
		}

		@Override
		public Object getValidatedValue() {
			return validatedValue;
		}

		@Override
		public <T> T unwrap(Class<T> type) {
			try {
				return type.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new IllegalArgumentException(e);
			}
		}

	}
}
