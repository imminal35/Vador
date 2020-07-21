/*
 * Copyright 2020 salesforce.com, inc. 
 * All Rights Reserved 
 * Company Confidential
 */

package consumer.config;


import consumer.BaseService;
import consumer.Service;
import consumer.bean.Parent;
import consumer.failure.ValidationFailure;
import io.vavr.collection.List;
import org.qtc.delphinus.types.validators.Validator;


/**
 * gakshintala created on 4/13/20.
 */
public class ServiceConfig {
    BaseService<Parent> getService(List<Validator<Parent, ValidationFailure>> requestValidators) {
        final Service service = new Service();
        service.setRequestValidators(requestValidators);
        return service;
    }
}
