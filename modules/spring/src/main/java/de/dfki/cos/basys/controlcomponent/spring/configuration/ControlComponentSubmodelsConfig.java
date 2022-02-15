package de.dfki.cos.basys.controlcomponent.spring.configuration;

import de.dfki.cos.basys.aas.component.AasComponentContext;
import de.dfki.cos.basys.aas.services.VABHTTPCorsInterface;
import de.dfki.cos.basys.controlcomponent.ControlComponent;
import de.dfki.cos.basys.controlcomponent.server.aas.ControlComponentSubmodelFactory;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.aas.registration.api.IAASRegistry;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IdentifierType;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier;
import org.eclipse.basyx.submodel.restapi.SubmodelProvider;
import org.eclipse.basyx.vab.exception.provider.ProviderException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.http.HttpServlet;

@Configuration
public class ControlComponentSubmodelsConfig {

    @Value("${server.accessibleEndpoint}")
    private String accessibleEndpoint;

    @Value("${basys.eventTransmitter.serviceConnectionString}")
    private String etServiceConnectionString;
//
//    @Value("${basys.controlcomponent.aas.asset-id}")
//    private String assetId;
//
//    @Value("${basys.controlcomponent.aas.id}")
//    private String assId;
//
//    @Value("${basys.controlcomponent.aas.submodel.interace.id}")
//    private String ccInterfaceSubmodelId;
//
//    @Value("${basys.controlcomponent.aas.submodel.instance.id}")
//    private String ccInstanceSubmodelId;

    @Autowired
    private ControlComponent controlComponent;

    @Autowired
    private IAASRegistry aasRegistry;

    @Bean
    public Submodel ccInstanceSubmodel() {
        return ControlComponentSubmodelFactory.createInstanceSubmodel(controlComponent, etServiceConnectionString);
    }

    @Bean
    public Submodel ccInterfaceSubmodel() {
        return ControlComponentSubmodelFactory.createInterfaceSubmodel(controlComponent);
    }

    @Bean
    public ServletRegistrationBean ccInstanceSubmodelServletBean() {
        Submodel sm = ccInstanceSubmodel();
        HttpServlet servlet = new VABHTTPCorsInterface(new SubmodelProvider(sm));
        String path = "instance"; // sm.getIdShort()
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/"+ path + "/*");
        bean.setName(path); // important to avoid overriding, see https://stackoverflow.com/questions/30670327/spring-boot-with-multiple-dispatcherservlet-each-having-their-own-controllers/30686733
        bean.setLoadOnStartup(1);

        String aasId = ControlComponentSubmodelFactory.getAasId(controlComponent);
        registerSubmodel(aasId, path, sm);

        return bean;
    }

    @Bean
    public ServletRegistrationBean ccInterfaceSubmodelServletBean() {
        Submodel sm = ccInterfaceSubmodel();
        HttpServlet servlet = new VABHTTPCorsInterface(new SubmodelProvider(sm));
        String path = "interface"; // sm.getIdShort()
        ServletRegistrationBean bean = new ServletRegistrationBean(servlet, "/"+ path + "/*");
        bean.setName(path);
        bean.setLoadOnStartup(1);

        String aasId = ControlComponentSubmodelFactory.getAasId(controlComponent);
        registerSubmodel(aasId, path, sm);

        return bean;
    }

    public void registerSubmodel(String aasId, String path, Submodel sm) {
        try {
            aasRegistry.delete(new Identifier(IdentifierType.CUSTOM, aasId), sm.getIdentification());
        } catch (ProviderException var5) {
        }

        try {
            SubmodelDescriptor smDescriptor = new SubmodelDescriptor(sm, this.accessibleEndpoint + "/" + path + "/submodel");
            aasRegistry.register(new Identifier(IdentifierType.CUSTOM, aasId), smDescriptor);
        } catch (ProviderException e) {
            e.printStackTrace();
        }
    }
}
