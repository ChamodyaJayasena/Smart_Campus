package com.smartcampus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaRedirectController {

    @RequestMapping(value = {
        "/",
        "/home",
        "/login",
        "/signup",
        "/admin-login",
        "/dashboard",
        "/technician",
        "/admin"
    })
    public String redirect() {
        return "forward:/index.html";
    }
}
