package nz.co.twg.{{cookiecutter.java_package_name}}.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import nz.co.twg.{{cookiecutter.java_package_name}}.services.SampleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SampleControllerTest {

    @Mock private SampleService service;

    // object under test
    private SampleController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        this.controller = new SampleController(service);
    }

    @Test
    void test() {
        // given
        when(service.featureDependentLogic()).thenReturn("test");

        // when
        String result = this.controller.featureDependentEndpoint();

        // then
        verify(service).featureDependentLogic();
        assertEquals("test", result);
    }
}
