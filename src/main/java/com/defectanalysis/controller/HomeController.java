package com.defectanalysis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/input")
    public String inputForm() {
        return "input-form";
    }
//    @GetMapping("/inputTest")
//    public String inputFormTest() {
//        return "input-formTest";
//    }
}