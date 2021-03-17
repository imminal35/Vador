/*
 * Copyright 2019 salesforce.com, inc.
 * All Rights Reserved
 * Company Confidential
 */

package consumer.validators.simple;


import consumer.bean.Child;
import consumer.failure.ValidationFailure;
import org.qtc.delphinus.types.validators.SimpleValidator;

import static consumer.failure.ValidationFailureMessage.FIELD_NULL_OR_EMPTY;

public class ChildRequestValidator {

    static final String ERROR_LABEL_PARAM_PAYMENT_AUTHORIZATION_ID
            = "PaymentStandardFields.PaymentAuthorizationId.getName()";
    /**
     * Validates if Auth id in request has a status PROCESSED.
     * This is a lambda function implementation.
     */
    public static final SimpleValidator<Child, ValidationFailure> validation1 =
            child -> {
                if (child == null) {
                    return null;
                } else {
                    return new ValidationFailure(FIELD_NULL_OR_EMPTY);
                }
            };

    static final SimpleValidator<Child, ValidationFailure> validation2 =
            child -> {
                if (child == null) {
                    return null;
                } else {
                    return new ValidationFailure(FIELD_NULL_OR_EMPTY);
                }
            };
}