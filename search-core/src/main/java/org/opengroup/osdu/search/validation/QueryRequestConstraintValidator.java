// Copyright 2017-2019, Schlumberger
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.search.validation;

import java.util.Set;

import jakarta.validation.Configuration;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.opengroup.osdu.core.common.model.search.QueryRequest;

public class QueryRequestConstraintValidator implements ConstraintValidatorFactory {

	private final ConstraintValidatorFactory defaultFactory =
		Validation.byDefaultProvider().configure().getDefaultConstraintValidatorFactory();

	@Override
	public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
		return defaultFactory.getInstance(key);
	}

	@Override
	public void releaseInstance(ConstraintValidator<?, ?> instance) {
		defaultFactory.releaseInstance(instance);
	}

	public Validator getValidator() {
		Configuration<?> config = Validation.byDefaultProvider().configure();
		config.constraintValidatorFactory(this);
		ValidatorFactory factory = config.buildValidatorFactory();
		return factory.getValidator();
	}

	public String isValid(QueryRequest queryRequest) {
		String violationMessage = null;
		Set<ConstraintViolation<QueryRequest>> violations = getValidator().validate(queryRequest);
		if (violations.isEmpty())
			return null;

		for (ConstraintViolation<QueryRequest> violation : violations) {
			violationMessage = violation.getMessage();
		}
		return violationMessage;
	}
}
