package org.revcloud.vader.lift;

import static consumer.failure.ValidationFailure.NONE;
import static consumer.failure.ValidationFailure.UNKNOWN_EXCEPTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.revcloud.vader.lift.InheritanceLiftEtrUtil.liftAllToChildValidatorType;

import consumer.failure.ValidationFailure;
import io.vavr.Tuple;
import io.vavr.control.Either;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.revcloud.vader.runner.Runner;
import org.revcloud.vader.runner.ValidationConfig;
import org.revcloud.vader.types.validators.Validator;
import org.revcloud.vader.types.validators.ValidatorEtr;

class InheritanceLiftEtrUtilKtTest {

  @Test
  void liftSimpleParentToChildValidatorTypeTest() {
    final Validator<Parent, ValidationFailure> v1 = ignore -> NONE;
    final Validator<Child, ValidationFailure> v2 = ignore -> UNKNOWN_EXCEPTION;
    final var validationConfig =
        ValidationConfig.<Child, ValidationFailure>toValidate()
            .withValidators(Tuple.of(List.of(v1, v2), NONE))
            .prepare();
    final var result = Runner.validateAndFailFast(new Child(), validationConfig);
    assertThat(result).contains(UNKNOWN_EXCEPTION);
  }

  @Test
  void liftParentToChildValidatorTypeTest() {
    final ValidatorEtr<Parent, ValidationFailure> v1 = ignore -> Either.right(NONE);
    final ValidatorEtr<Parent, ValidationFailure> v2 = ignore -> Either.right(NONE);
    final ValidatorEtr<Child, ValidationFailure> v3 = ignore -> Either.left(UNKNOWN_EXCEPTION);
    final var validationConfig =
        ValidationConfig.<Child, ValidationFailure>toValidate()
            .withValidatorEtrs(liftAllToChildValidatorType(List.of(v1, v2)))
            .withValidatorEtr(v3)
            .prepare();
    final var result = Runner.validateAndFailFast(new Child(), validationConfig);
    assertThat(result).contains(UNKNOWN_EXCEPTION);
  }

  private abstract class Parent {}

  @Data
  @EqualsAndHashCode(callSuper = true)
  private class Child extends Parent {}
}