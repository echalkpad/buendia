package org.projectbuendia.openmrs.webservices.rest;

import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.MainResourceController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/rest/" + RestController.REST_VERSION_1_AND_NAMESPACE)
public class RestController extends MainResourceController {
 
	public static final String REST_VERSION_1_AND_NAMESPACE = RestConstants.VERSION_1 + "/projectbuendia";
	
    /**
     * @see org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController#getNamespace()
     */
    @Override
    public String getNamespace() {
        return "v1/basicexample";
    }
}