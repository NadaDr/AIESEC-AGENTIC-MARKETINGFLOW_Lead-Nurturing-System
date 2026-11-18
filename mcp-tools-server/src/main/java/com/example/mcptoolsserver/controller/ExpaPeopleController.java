
package com.example.mcptoolsserver.controller;

import com.example.mcptoolsserver.service.ExpaPeopleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/expa")
public class ExpaPeopleController {

    private final ExpaPeopleService expaPeopleService;

    public ExpaPeopleController(ExpaPeopleService expaPeopleService) {

        this.expaPeopleService = expaPeopleService;
    }

    @GetMapping("/people")
    public Map<String, Object> getPeople(
            @RequestParam(defaultValue = "1") int page) {

        return expaPeopleService.getPeoplePage(page);
    }

    @GetMapping("/people/year")
    public Map<String, Object> getPeopleByYear(
            @RequestParam int year) {

        return expaPeopleService.getPeopleByYear(year);
    }
}
