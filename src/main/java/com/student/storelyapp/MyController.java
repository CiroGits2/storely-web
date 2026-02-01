package com.student.storelyapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {
    
    @GetMapping("/storelyapp")
    public String welcomeText(@RequestParam(name = "name") String name) {
        //String welcomeInEnglish = "Welcome to Storely Offical Website ";
        //String welcomeInSpanish = "Bienvenido/a al sitio oficial de Storely!";

        return "Welcome to Storely Offical Website " + name;
    }
}